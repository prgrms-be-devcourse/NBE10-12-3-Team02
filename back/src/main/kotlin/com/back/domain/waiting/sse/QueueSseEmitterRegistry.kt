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

    private val emitters =
        ConcurrentHashMap<Long, ConcurrentHashMap<Long, MutableList<SynchronizedEmitter>>>()

    fun register(scheduleId: Long, userId: Long, emitter: SseEmitter): SynchronizedEmitter {
        val wrapper = SynchronizedEmitter(emitter)
        emitters
            .computeIfAbsent(scheduleId) { ConcurrentHashMap() }
            .computeIfAbsent(userId) { CopyOnWriteArrayList() }
            .add(wrapper)

        val cleanup = Runnable { remove(scheduleId, userId, wrapper) }
        emitter.onCompletion(cleanup)
        emitter.onTimeout(cleanup)
        emitter.onError { cleanup.run() }

        log.debug("대기열 SSE 구독 등록: scheduleId={}, userId={}", scheduleId, userId)
        return wrapper
    }

    fun broadcastStatus(scheduleId: Long, event: QueueStatusEvent) {
        subscribers(scheduleId).forEach { (userId, wrapper) ->
            sendOrRemove(
                scheduleId,
                userId,
                wrapper,
                SseEmitter.event()
                    .name(QUEUE_STATUS_EVENT)
                    .data(event),
            )
        }
    }

    fun sendEntryAllowed(event: EntryAllowedEvent) {
        emitters[event.scheduleId]?.get(event.userId)?.toList()?.forEach { wrapper ->
            sendAndComplete(
                event.scheduleId,
                event.userId,
                wrapper,
                SseEmitter.event()
                    .name(ENTRY_ALLOWED_EVENT)
                    .data(event),
            )
        }
    }

    fun sendError(event: QueueErrorEvent) {
        if (event.userId == null) {
            subscribers(event.scheduleId).forEach { (userId, wrapper) ->
                sendAndComplete(
                    event.scheduleId,
                    userId,
                    wrapper,
                    SseEmitter.event()
                        .name(QUEUE_ERROR_EVENT)
                        .data(event),
                )
            }
            return
        }

        emitters[event.scheduleId]?.get(event.userId)?.toList()?.forEach { wrapper ->
            sendOrRemove(
                event.scheduleId,
                event.userId,
                wrapper,
                SseEmitter.event()
                    .name(QUEUE_ERROR_EVENT)
                    .data(event),
            )
        }
    }

    fun sendHeartbeat() {
        emitters.entries.toList().forEach { (scheduleId, users) ->
            users.entries.toList().forEach { (userId, wrappers) ->
                wrappers.toList().forEach { wrapper ->
                    sendOrRemove(
                        scheduleId,
                        userId,
                        wrapper,
                        SseEmitter.event()
                            .name(HEARTBEAT_EVENT)
                            .data(mapOf("serverTime" to Instant.now().toString())),
                    )
                }
            }
        }
    }

    private fun subscribers(scheduleId: Long): List<Pair<Long, SynchronizedEmitter>> {
        val users = emitters[scheduleId] ?: return emptyList()
        return users.entries
            .asSequence()
            .flatMap { (userId, wrappers) -> wrappers.asSequence().map { userId to it } }
            .toList()
    }

    private fun sendOrRemove(
        scheduleId: Long,
        userId: Long,
        wrapper: SynchronizedEmitter,
        event: SseEmitter.SseEventBuilder,
    ) {
        try {
            wrapper.send(event)
        } catch (e: Exception) {
            log.debug(
                "대기열 SSE 전송 실패로 연결 제거: scheduleId={}, userId={}, error={}",
                scheduleId,
                userId,
                e.message,
            )
            handleSendFailure(scheduleId, userId, wrapper, e)
        }
    }

    private fun sendAndComplete(
        scheduleId: Long,
        userId: Long,
        wrapper: SynchronizedEmitter,
        event: SseEmitter.SseEventBuilder,
    ) {
        try {
            wrapper.send(event)
            remove(scheduleId, userId, wrapper)
            wrapper.emitter.complete()
        } catch (e: Exception) {
            log.debug(
                "대기열 SSE 최종 이벤트 전송 실패로 연결 제거: scheduleId={}, userId={}, error={}",
                scheduleId,
                userId,
                e.message,
            )
            handleSendFailure(scheduleId, userId, wrapper, e)
        }
    }

    private fun handleSendFailure(
        scheduleId: Long,
        userId: Long,
        wrapper: SynchronizedEmitter,
        exception: Exception,
    ) {
        remove(scheduleId, userId, wrapper)
        runCatching { wrapper.emitter.completeWithError(exception) }
    }

    private fun remove(scheduleId: Long, userId: Long, wrapper: SynchronizedEmitter) {
        emitters.computeIfPresent(scheduleId) { _, users ->
            users.computeIfPresent(userId) { _, wrappers ->
                wrappers.remove(wrapper)
                wrappers.takeIf { it.isNotEmpty() }
            }
            users.takeIf { it.isNotEmpty() }
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
