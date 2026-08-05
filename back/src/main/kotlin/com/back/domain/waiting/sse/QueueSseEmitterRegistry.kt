package com.back.domain.waiting.sse

import com.back.domain.waiting.event.EntryAllowedEvent
import com.back.domain.waiting.event.QueueErrorEvent
import com.back.domain.waiting.event.QueueStatusEvent
import com.back.domain.waiting.dto.QueueConnectionEvent
import com.back.domain.waiting.constant.QueueConnectionState
import com.back.domain.waiting.service.QueueConnectionSnapshot
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
        val concertId: Long?,
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

    fun register(
        scheduleId: Long,
        userId: Long,
        emitter: SseEmitter,
        concertId: Long? = null,
    ): SynchronizedEmitter {
        val wrapper = SynchronizedEmitter(emitter, concertId)
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
                    .name(QueueSseEventName.QUEUE_STATUS)
                    .data(event),
            )
        }
    }

    fun sendEntryAllowed(event: EntryAllowedEvent): QueueSseDeliveryResult {
        val wrappers = emitters[event.scheduleId]?.get(event.userId)?.toList().orEmpty()
        if (wrappers.isEmpty()) return QueueSseDeliveryResult.NO_SUBSCRIBER

        val deliveredCount = wrappers.count { wrapper ->
            sendAndComplete(
                event.scheduleId,
                event.userId,
                wrapper,
                SseEmitter.event()
                    .name(QueueSseEventName.ENTRY_ALLOWED)
                    .data(event),
            )
        }
        return deliveryResult(deliveredCount)
    }

    fun sendError(event: QueueErrorEvent): QueueSseDeliveryResult {
        if (event.userId == null) {
            val subscribers = subscribers(event.scheduleId)
            if (subscribers.isEmpty()) return QueueSseDeliveryResult.NO_SUBSCRIBER

            val deliveredCount = subscribers.count { (userId, wrapper) ->
                sendAndComplete(
                    event.scheduleId,
                    userId,
                    wrapper,
                    SseEmitter.event()
                        .name(QueueSseEventName.QUEUE_ERROR)
                        .data(event),
                )
            }
            return deliveryResult(deliveredCount)
        }

        val wrappers = emitters[event.scheduleId]?.get(event.userId)?.toList().orEmpty()
        if (wrappers.isEmpty()) return QueueSseDeliveryResult.NO_SUBSCRIBER

        val deliveredCount = wrappers.count { wrapper ->
            sendOrRemove(
                event.scheduleId,
                event.userId,
                wrapper,
                SseEmitter.event()
                    .name(QueueSseEventName.QUEUE_ERROR)
                    .data(event),
            )
        }
        return deliveryResult(deliveredCount)
    }

    fun sendHeartbeat() = sendHeartbeat { _, _ -> null }

    fun sendHeartbeat(
        stateResolver: (scheduleId: Long, userId: Long) -> QueueConnectionSnapshot?,
    ) {
        emitters.entries.toList().forEach { (scheduleId, users) ->
            users.entries.toList().forEach { (userId, wrappers) ->
                val snapshot = runCatching {
                    stateResolver(scheduleId, userId)
                }.onFailure { exception ->
                    // Redis 상태 조회 실패만으로 정상 SSE 연결을 제거하지 않는다.
                    log.warn(
                        "대기열 heartbeat 상태 조회 실패, 연결 유지: scheduleId={}, userId={}, error={}",
                        scheduleId,
                        userId,
                        exception.message,
                    )
                }.getOrNull()

                wrappers.toList().forEach { wrapper ->
                    if (snapshot is QueueConnectionSnapshot.Active && wrapper.concertId != null) {
                        sendAndComplete(
                            scheduleId,
                            userId,
                            wrapper,
                            SseEmitter.event()
                                .name(QueueSseEventName.CONNECTED)
                                .data(
                                    QueueConnectionEvent(
                                        concertId = wrapper.concertId,
                                        scheduleId = scheduleId,
                                        userId = userId,
                                        state = QueueConnectionState.ACTIVE,
                                        rank = 0L,
                                        myQueueNumber = 0L,
                                        entryToken = snapshot.entryToken,
                                    ),
                                ),
                        )
                    } else {
                        sendOrRemove(
                            scheduleId,
                            userId,
                            wrapper,
                            SseEmitter.event()
                                .name(QueueSseEventName.HEARTBEAT)
                                .data(mapOf("serverTime" to Instant.now().toString())),
                        )
                    }
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
    ): Boolean = try {
        wrapper.send(event)
        true
    } catch (e: Exception) {
        log.debug(
            "대기열 SSE 전송 실패로 연결 제거: scheduleId={}, userId={}, error={}",
            scheduleId,
            userId,
            e.message,
        )
        handleSendFailure(scheduleId, userId, wrapper, e)
        false
    }

    private fun sendAndComplete(
        scheduleId: Long,
        userId: Long,
        wrapper: SynchronizedEmitter,
        event: SseEmitter.SseEventBuilder,
    ): Boolean = try {
        wrapper.send(event)
        remove(scheduleId, userId, wrapper)
        wrapper.emitter.complete()
        true
    } catch (e: Exception) {
        log.debug(
            "대기열 SSE 최종 이벤트 전송 실패로 연결 제거: scheduleId={}, userId={}, error={}",
            scheduleId,
            userId,
            e.message,
        )
        handleSendFailure(scheduleId, userId, wrapper, e)
        false
    }

    private fun deliveryResult(deliveredCount: Int): QueueSseDeliveryResult =
        if (deliveredCount > 0) QueueSseDeliveryResult.DELIVERED else QueueSseDeliveryResult.FAILED

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
        private val log = LoggerFactory.getLogger(QueueSseEmitterRegistry::class.java)
    }
}
