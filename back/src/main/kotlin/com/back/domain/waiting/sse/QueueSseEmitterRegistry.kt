package com.back.domain.waiting.sse

import com.back.domain.queue.event.EntryAllowedEvent
import com.back.domain.queue.event.QueueErrorEvent
import com.back.domain.queue.event.QueueStatusEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class QueueSseEmitterRegistry {
    data class SubscriberKey(
        val scheduleId: Long,
        val userId: Long,
    )

    class SynchronizedEmitter(
        val emitter: SseEmitter,
        private val lock: ReentrantLock = ReentrantLock(),
    ) {
        fun send(event: SseEmitter.SseEventBuilder) {
            lock.withLock {
                emitter.send(event)
            }
        }
    }

    private val emitters = ConcurrentHashMap<SubscriberKey, MutableList<SynchronizedEmitter>>()

    fun register(scheduleId: Long, userId: Long, emitter: SseEmitter): SynchronizedEmitter {
        val key = SubscriberKey(scheduleId, userId)
        val wrapper = SynchronizedEmitter(emitter)
        emitters.computeIfAbsent(key) { CopyOnWriteArrayList() }.add(wrapper)

        val cleanup = Runnable { remove(key, wrapper) }
        emitter.onCompletion(cleanup)
        emitter.onTimeout(cleanup)
        emitter.onError { cleanup.run() }

        log.debug("대기열 SSE 구독 등록: scheduleId={}, userId={}", scheduleId, userId)
        return wrapper
    }

    fun broadcastStatus(scheduleId: Long, event: QueueStatusEvent) {
        subscribers(scheduleId).forEach { (key, wrapper) ->
            sendOrRemove(
                key,
                wrapper,
                SseEmitter.event()
                    .name(QUEUE_STATUS_EVENT)
                    .data(event),
            )
        }
    }

    fun sendEntryAllowed(event: EntryAllowedEvent) {
        val key = SubscriberKey(event.scheduleId, event.userId)
        emitters[key]?.toList()?.forEach { wrapper ->
            sendOrRemove(
                key,
                wrapper,
                SseEmitter.event()
                    .name(ENTRY_ALLOWED_EVENT)
                    .data(event),
            )
        }
    }

    fun sendError(event: QueueErrorEvent) {
        if (event.userId == null) {
            subscribers(event.scheduleId).forEach { (key, wrapper) ->
                sendOrRemove(
                    key,
                    wrapper,
                    SseEmitter.event()
                        .name(QUEUE_ERROR_EVENT)
                        .data(event),
                )
            }
            return
        }

        val key = SubscriberKey(event.scheduleId, event.userId)
        emitters[key]?.toList()?.forEach { wrapper ->
            sendOrRemove(
                key,
                wrapper,
                SseEmitter.event()
                    .name(QUEUE_ERROR_EVENT)
                    .data(event),
            )
        }
    }

    fun sendHeartbeat() {
        emitters.entries.toList().forEach { (key, wrappers) ->
            wrappers.toList().forEach { wrapper ->
                sendOrRemove(
                    key,
                    wrapper,
                    SseEmitter.event()
                        .name(HEARTBEAT_EVENT)
                        .data(mapOf("serverTime" to Instant.now().toString())),
                )
            }
        }
    }

    private fun subscribers(scheduleId: Long): List<Pair<SubscriberKey, SynchronizedEmitter>> =
        emitters.entries
            .asSequence()
            .filter { it.key.scheduleId == scheduleId }
            .flatMap { entry -> entry.value.asSequence().map { entry.key to it } }
            .toList()

    private fun sendOrRemove(
        key: SubscriberKey,
        wrapper: SynchronizedEmitter,
        event: SseEmitter.SseEventBuilder,
    ) {
        try {
            wrapper.send(event)
        } catch (e: Exception) {
            log.debug(
                "대기열 SSE 전송 실패로 연결 제거: scheduleId={}, userId={}, error={}",
                key.scheduleId,
                key.userId,
                e.message,
            )
            remove(key, wrapper)
            runCatching { wrapper.emitter.completeWithError(e) }
        }
    }

    private fun remove(key: SubscriberKey, wrapper: SynchronizedEmitter) {
        emitters.computeIfPresent(key) { _, wrappers ->
            wrappers.remove(wrapper)
            wrappers.takeIf { it.isNotEmpty() }
        }
    }

    companion object {
        const val CONNECTED_EVENT = "connected"
        const val QUEUE_STATUS_EVENT = "queue-status"
        const val ENTRY_ALLOWED_EVENT = "entry-allowed"
        const val QUEUE_ERROR_EVENT = "queue-error"
        const val HEARTBEAT_EVENT = "heartbeat"

        private val log = LoggerFactory.getLogger(QueueSseEmitterRegistry::class.java)
    }
}
