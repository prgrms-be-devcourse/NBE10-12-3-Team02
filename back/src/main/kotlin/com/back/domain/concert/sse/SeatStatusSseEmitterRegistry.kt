package com.back.domain.concert.sse

import com.back.domain.concert.sse.repository.SseOutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

@Component
class SeatStatusSseEmitterRegistry(
    private val eventCache: SeatStatusSseEventCache,
    private val sseOutboxEventRepository: SseOutboxEventRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    class SynchronizedEmitter(
        val emitter: SseEmitter,
        val lock: ReentrantLock = ReentrantLock(),
        val createdAt: Long = System.currentTimeMillis(),
        val lastSentAt: AtomicLong = AtomicLong(System.currentTimeMillis())
    ) {
        fun send(event: SseEmitter.SseEventBuilder, timeoutMs: Long = 500L) {
            val acquired = lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)
            if (!acquired) {
                throw IOException("Emitter lock acquisition timed out (${timeoutMs}ms) - Slow Consumer detected")
            }
            try {
                emitter.send(event)
                lastSentAt.set(System.currentTimeMillis())
            } finally {
                lock.unlock()
            }
        }
    }

    private val emitters = ConcurrentHashMap<Long, MutableList<SynchronizedEmitter>>()

    fun register(scheduleId: Long, emitter: SseEmitter, lastEventId: String? = null): SynchronizedEmitter {
        val wrapper = SynchronizedEmitter(emitter)
        emitters.computeIfAbsent(scheduleId) { CopyOnWriteArrayList() }.add(wrapper)

        val cleanup = Runnable {
            val list = emitters[scheduleId]
            list?.remove(wrapper)
        }
        emitter.onCompletion(cleanup)
        emitter.onTimeout(cleanup)
        emitter.onError { cleanup.run() }

        // Last-Event-ID 재연결 시 DB/캐시 기반 누락 이벤트 Replay 전송
        if (!lastEventId.isNullOrBlank()) {
            val missedEvents = getMissedEvents(scheduleId, lastEventId)
            for ((eventId, seatNumber, status) in missedEvents) {
                try {
                    val data = "{\"seatNumber\":\"$seatNumber\",\"status\":\"$status\"}"
                    wrapper.send(
                        SseEmitter.event()
                            .id(eventId)
                            .name("seat_status_changed")
                            .data(data)
                    )
                } catch (e: Exception) {
                    log.warn("누락 이벤트 Replay 전송 실패: scheduleId={}, eventId={}", scheduleId, eventId)
                    break
                }
            }
        }

        log.debug("SSE 구독 등록: scheduleId={}, 총 구독자={}", scheduleId, emitters[scheduleId]?.size ?: 0)
        return wrapper
    }

    private fun getMissedEvents(scheduleId: Long, lastEventId: String): List<Triple<String, String, String>> {
        val outboxEvent = sseOutboxEventRepository.findByEventId(lastEventId)
        val targetId = outboxEvent?.id
        if (targetId != null) {
            val dbEvents = sseOutboxEventRepository.findByScheduleIdAndIdGreaterThanOrderByIdAsc(scheduleId, targetId)
            return dbEvents.map { Triple(it.eventId, it.seatNumber, it.status) }
        }

        return eventCache.getEventsAfter(scheduleId, lastEventId)
            .map { Triple(it.eventId, it.seatNumber, it.status) }
    }

    fun broadcast(scheduleId: Long, seatNumber: String, status: String) {
        val list = emitters[scheduleId]
        if (list.isNullOrEmpty()) return

        val cachedEvent = eventCache.addEvent(scheduleId, seatNumber, status)
        val data = "{\"seatNumber\":\"$seatNumber\",\"status\":\"$status\"}"

        for (wrapper in list) {
            executor.submit {
                try {
                    wrapper.send(
                        SseEmitter.event()
                            .id(cachedEvent.eventId)
                            .name("seat_status_changed")
                            .data(data)
                    )
                } catch (e: Exception) {
                    log.warn("SSE 전송 실패 (자원 정리): scheduleId={}, seat={}, err={}", scheduleId, seatNumber, e.message)
                    list.remove(wrapper)
                    runCatching { wrapper.emitter.completeWithError(e) }
                }
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    fun sendHeartbeat() {
        for ((scheduleId, list) in emitters) {
            if (list.isEmpty()) continue

            for (wrapper in list) {
                executor.submit {
                    try {
                        wrapper.send(
                            SseEmitter.event()
                                .name("heartbeat")
                                .comment("ping")
                                .data("ping")
                        )
                    } catch (e: Exception) {
                        log.debug("Heartbeat 전송 실패 (좀비 Emitter 정리): scheduleId={}, err={}", scheduleId, e.message)
                        list.remove(wrapper)
                        runCatching { wrapper.emitter.completeWithError(e) }
                    }
                }
            }
        }
    }
}
