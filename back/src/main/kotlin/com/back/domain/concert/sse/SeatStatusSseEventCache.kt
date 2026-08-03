package com.back.domain.concert.sse

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong

data class SseSeatEvent(
    val eventId: String,
    val scheduleId: Long,
    val seatNumber: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Component
class SeatStatusSseEventCache {
    private val log = LoggerFactory.getLogger(javaClass)
    private val sequenceMap = ConcurrentHashMap<Long, AtomicLong>()
    private val cacheMap = ConcurrentHashMap<Long, ConcurrentLinkedDeque<SseSeatEvent>>()

    fun addEvent(scheduleId: Long, seatNumber: String, status: String, customEventId: String? = null): SseSeatEvent {
        val eventId = customEventId ?: run {
            val seq = sequenceMap.computeIfAbsent(scheduleId) { AtomicLong(0) }.incrementAndGet()
            "$scheduleId:${seq.toString().padStart(16, '0')}"
        }
        val event = SseSeatEvent(eventId, scheduleId, seatNumber, status)

        val deque = cacheMap.computeIfAbsent(scheduleId) { ConcurrentLinkedDeque() }
        deque.addLast(event)

        while (deque.size > MAX_CACHE_SIZE) {
            deque.pollFirst()
        }

        return event
    }

    fun getEventsAfter(scheduleId: Long, lastEventId: String?): List<SseSeatEvent> {
        if (lastEventId.isNullOrBlank()) return emptyList()
        val deque = cacheMap[scheduleId] ?: return emptyList()

        val events = deque.toList()
        val targetIndex = events.indexOfFirst { it.eventId == lastEventId }

        return if (targetIndex != -1) {
            events.drop(targetIndex + 1)
        } else {
            events
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    fun cleanExpiredCache() {
        val expireTime = System.currentTimeMillis() - CACHE_TTL_MS
        var totalRemoved = 0

        for ((scheduleId, deque) in cacheMap) {
            deque.removeIf { it.timestamp < expireTime }
            if (deque.isEmpty()) {
                cacheMap.remove(scheduleId)
                sequenceMap.remove(scheduleId)
                totalRemoved++
            }
        }
        if (totalRemoved > 0) {
            log.info("만료된 SSE In-Memory 이벤트 캐시 맵 정돈 완료: {}개 회차 정리", totalRemoved)
        }
    }

    companion object {
        private const val MAX_CACHE_SIZE = 100
        private const val CACHE_TTL_MS = 60 * 60 * 1000L // 1시간
    }
}
