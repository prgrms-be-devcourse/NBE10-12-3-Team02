package com.back.domain.waiting.outbox.dto

import com.back.domain.waiting.outbox.constant.QueueSseOutboxEventType
import java.time.LocalDateTime

data class QueueSseOutboxEventData(
    val eventId: String,
    val eventType: QueueSseOutboxEventType,
    val payload: String,
    val expiresAt: LocalDateTime,
) {
    fun isExpired(now: LocalDateTime): Boolean = !expiresAt.isAfter(now)
}
