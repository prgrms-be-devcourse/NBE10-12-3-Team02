package com.back.domain.concert.sse

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class SeatStatusSseEmitterRegistry {
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

    fun register(scheduleId: Long, emitter: SseEmitter): SynchronizedEmitter {
        val wrapper = SynchronizedEmitter(emitter)
        emitters.computeIfAbsent(scheduleId) { CopyOnWriteArrayList() }.add(wrapper)

        val cleanup = Runnable {
            val list = emitters[scheduleId]
            list?.remove(wrapper)
        }
        emitter.onCompletion(cleanup)
        emitter.onTimeout(cleanup)
        emitter.onError { cleanup.run() }

        log.debug("SSE 구독 등록: scheduleId={}, 총 구독자={}", scheduleId, emitters[scheduleId]?.size ?: 0)

        return wrapper
    }

    fun broadcast(scheduleId: Long, seatNumber: String, status: String) {
        val list = emitters[scheduleId]
        if (list.isNullOrEmpty()) return

        val data = "{\"seatNumber\":\"$seatNumber\",\"status\":\"$status\"}"

        for (wrapper in list) {
            try {
                wrapper.send(
                    SseEmitter.event()
                        .name("seat_status_changed")
                        .data(data)
                )
            } catch (e: Exception) {
                log.warn("SSE 전송 연동 경고 (스킵): scheduleId={}, seat={}, err={}", scheduleId, seatNumber, e.message)
            }
        }
    }
}
