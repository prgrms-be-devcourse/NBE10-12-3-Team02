package com.back.domain.waiting.outbox.service

import com.back.domain.waiting.outbox.config.QueueSseOutboxProperties
import com.back.domain.waiting.outbox.constant.QueueSseOutboxStatus
import com.back.domain.waiting.outbox.dto.QueueSseOutboxEventData
import com.back.domain.waiting.outbox.entity.QueueSseOutboxEvent
import com.back.domain.waiting.outbox.repository.QueueSseOutboxRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class QueueSseOutboxTransactionService(
    private val repository: QueueSseOutboxRepository,
    private val properties: QueueSseOutboxProperties,
) {
    @Transactional(readOnly = true)
    fun loadProcessingEvent(eventId: String): QueueSseOutboxEventData? =
        repository.findByEventId(eventId)
            ?.takeIf { it.status == QueueSseOutboxStatus.PROCESSING }
            ?.let {
                QueueSseOutboxEventData(
                    eventId = it.eventId,
                    eventType = it.eventType,
                    payload = it.payload,
                    expiresAt = it.expiresAt,
                )
            }

    @Transactional
    fun markCompleted(eventId: String, now: LocalDateTime): Boolean =
        findProcessingEvent(eventId)?.let {
            it.complete(now)
            true
        } ?: false

    @Transactional
    fun markSkipped(eventId: String, now: LocalDateTime, reason: String): Boolean =
        findProcessingEvent(eventId)?.let {
            it.skip(now, reason)
            true
        } ?: false

    @Transactional
    fun markExpired(eventId: String, now: LocalDateTime): Boolean =
        findProcessingEvent(eventId)?.let {
            it.expire(now)
            true
        } ?: false

    @Transactional
    fun recordFailure(eventId: String, exception: Exception, now: LocalDateTime): Int? =
        findProcessingEvent(eventId)?.let {
            it.recordFailure(exception, now, properties.maxRetries, properties.retryDelay)
            it.retryCount
        }

    private fun findProcessingEvent(eventId: String): QueueSseOutboxEvent? =
        repository.findByEventId(eventId)
            ?.takeIf { it.status == QueueSseOutboxStatus.PROCESSING }
}
