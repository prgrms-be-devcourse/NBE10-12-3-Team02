package com.back.domain.notification.sse

import com.back.domain.notification.dto.NotificationPushPayload
import com.back.global.util.Ut
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class NotificationSseEmitterRegistry {
    private val log = LoggerFactory.getLogger(javaClass)

    class SynchronizedEmitter(
        val emitter: SseEmitter,
        val lock: ReentrantLock = ReentrantLock()
    ) {
        fun send(event: SseEmitter.SseEventBuilder) {
            lock.withLock {
                emitter.send(event)
            }
        }
    }

    private val emitters = ConcurrentHashMap<Long, MutableList<SynchronizedEmitter>>()

    fun register(userId: Long, emitter: SseEmitter): SynchronizedEmitter {
        val wrapper = SynchronizedEmitter(emitter)
        // compute()로 get-or-create와 add를 한 번에 묶어야, cleanup의 computeIfPresent와
        // 같은 key에 대해 서로 끼어들 여지 없이 원자적으로 처리된다.
        emitters.compute(userId) { _, list ->
            (list ?: CopyOnWriteArrayList()).apply { add(wrapper) }
        }

        val cleanup = Runnable {
            // list.remove와 "비었는지 확인 후 키 제거"를 한 번의 원자적 연산으로 묶어야
            // 그 사이에 register()가 끼어들어 새로 추가된 emitter가 유실되는 일이 없다.
            emitters.computeIfPresent(userId) { _, list ->
                list.remove(wrapper)
                if (list.isEmpty()) null else list
            }
        }
        emitter.onCompletion(cleanup)
        emitter.onTimeout(cleanup)
        emitter.onError { cleanup.run() }

        log.debug("SSE 구독 등록: userId={}, 총 구독자={}", userId, emitters[userId]?.size ?: 0)

        return wrapper
    }

    fun send(userId: Long, payload: NotificationPushPayload) {
        val list = emitters[userId]
        if (list.isNullOrEmpty()) return

        val data = Ut.json.toString(payload, "{}")

        for (wrapper in list) {
            try {
                wrapper.send(
                    SseEmitter.event()
                        .name("notification")
                        .data(data)
                )
            } catch (e: Exception) {
                log.warn("SSE 전송 실패 (자원 정리): userId={}, err={}", userId, e.message)
                runCatching { wrapper.emitter.completeWithError(e) }
            }
        }
    }
}
