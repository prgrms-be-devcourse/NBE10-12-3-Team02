package com.back.domain.concert.sse

import com.back.domain.concert.sse.repository.SseOutboxEventRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

@Component
class SeatStatusSseEmitterRegistry(
    private val eventCache: SeatStatusSseEventCache,
    private val sseOutboxEventRepository: SseOutboxEventRepository,
    // TaskExecutor 타입인 taskScheduler와 혼동되지 않도록 일반 비동기 작업용 실행기를 명시한다.
    @Qualifier("applicationTaskExecutor") private val taskExecutor: TaskExecutor,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    class SynchronizedEmitter(
        val emitter: SseEmitter,
        // ReentrantLock을 사용하여 Java 25 가상 스레드(Virtual Threads) 환경에서
        // synchronized 블록으로 인한 Carrier Thread 피닝(Pinning) 현상을 방지한다.
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

    // scheduleId별 현재 연결 수를 원자적으로 관리하는 카운터
    private val connectionCounts = ConcurrentHashMap<Long, AtomicInteger>()

    fun register(scheduleId: Long, emitter: SseEmitter, lastEventId: String? = null): SynchronizedEmitter? {
        val counter = connectionCounts.computeIfAbsent(scheduleId) { AtomicInteger(0) }
        val newCount = counter.incrementAndGet()
        if (newCount > MAX_CONNECTIONS_PER_SCHEDULE) {
            counter.decrementAndGet()
            log.warn("SSE 연결 수 상한 초과: scheduleId={}, count={}", scheduleId, newCount - 1)
            emitter.complete()
            return null
        }

        val wrapper = SynchronizedEmitter(emitter)
        emitters.computeIfAbsent(scheduleId) { CopyOnWriteArrayList() }.add(wrapper)

        val cleanup = Runnable {
            val list = emitters[scheduleId]
            list?.remove(wrapper)
            // 연결 수가 0으로 떨어지면 connectionCounts Map 엔트리를 원자적으로 제거한다.
            // remove(key, value) 오버로드는 현재 값이 counter와 동일할 때만 제거하므로,
            // 동시에 새 연결이 들어와 counter가 재사용되는 경우를 안전하게 처리한다.
            if (counter.decrementAndGet() == 0) {
                connectionCounts.remove(scheduleId, counter)
            }
        }
        emitter.onCompletion(cleanup)
        emitter.onTimeout(cleanup)
        emitter.onError { cleanup.run() }

        // Last-Event-ID 재연결 시 DB/캐시 기반 누락 이벤트 Replay 전송
        if (!lastEventId.isNullOrBlank()) {
            val missedEvents = getMissedEvents(scheduleId, lastEventId)
            for ((eventId, seatNumber, status) in missedEvents) {
                try {
                    val dataJson = objectMapper.writeValueAsString(mapOf("seatNumber" to seatNumber, "status" to status))
                    wrapper.send(
                        SseEmitter.event()
                            .id(eventId)
                            .name("seat_status_changed")
                            .data(dataJson)
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

    fun broadcast(scheduleId: Long, seatNumber: String, status: String, customEventId: String? = null) {
        val list = emitters[scheduleId]
        if (list.isNullOrEmpty()) return

        val cachedEvent = eventCache.addEvent(scheduleId, seatNumber, status, customEventId)
        val dataJson = objectMapper.writeValueAsString(mapOf("seatNumber" to seatNumber, "status" to status))

        for (wrapper in list) {
            executeTask {
                try {
                    wrapper.send(
                        SseEmitter.event()
                            .id(cachedEvent.eventId)
                            .name("seat_status_changed")
                            .data(dataJson)
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
                executeTask {
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

    private fun executeTask(task: Runnable) {
        val safeTask = Runnable {
            runCatching {
                task.run()
            }.onFailure { e ->
                log.error("SSE 비동기 전송 비정상 예외 발생: err={}", e.message, e)
            }
        }
        taskExecutor.execute(safeTask)
    }

    companion object {
        // 회차(scheduleId)당 최대 SSE 동시 연결 수
        private const val MAX_CONNECTIONS_PER_SCHEDULE = 10_000
    }
}
