package com.back.domain.concert.sse

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
    private val sequenceMap = ConcurrentHashMap<Long, AtomicLong>()
    private val cacheMap = ConcurrentHashMap<Long, ConcurrentLinkedDeque<SseSeatEvent>>()

    fun addEvent(scheduleId: Long, seatNumber: String, status: String): SseSeatEvent {
        val seq = sequenceMap.computeIfAbsent(scheduleId) { AtomicLong(0) }.incrementAndGet()
        val eventId = "$scheduleId:${seq.toString().padStart(16, '0')}"
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

    companion object {
        private const val MAX_CACHE_SIZE = 100
    }
}
