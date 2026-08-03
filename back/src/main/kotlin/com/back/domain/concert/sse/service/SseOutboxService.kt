package com.back.domain.concert.sse.service

import com.back.domain.concert.sse.entity.SseOutboxEvent
import com.back.domain.concert.sse.repository.SseOutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SseOutboxService(
    private val sseOutboxEventRepository: SseOutboxEventRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun saveOutboxEvent(scheduleId: Long, seatNumber: String, status: String): SseOutboxEvent? {
        return try {
            val eventId = "$scheduleId:${UUID.randomUUID()}"
            sseOutboxEventRepository.save(SseOutboxEvent(eventId, scheduleId, seatNumber, status))
        } catch (e: Exception) {
            log.warn("SseOutboxEvent 저장 실패: scheduleId={}, seat={}, err={}", scheduleId, seatNumber, e.message)
            null
        }
    }
}
