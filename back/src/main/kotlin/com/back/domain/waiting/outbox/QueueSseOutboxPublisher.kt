package com.back.domain.waiting.outbox

import com.back.domain.queue.event.EntryAllowedEvent
import com.back.domain.queue.event.QueueErrorEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class QueueSseOutboxPublisher(
    private val repository: QueueSseOutboxRepository,
    private val payloadCodec: QueueSseOutboxPayloadCodec,
    private val properties: QueueSseOutboxProperties,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun publishEntryAllowed(event: EntryAllowedEvent) {
        val expiresAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(event.expiredAt),
            ZoneId.systemDefault(),
        )
        save(QueueSseOutboxEventType.ENTRY_ALLOWED, event.scheduleId, event.userId, event, expiresAt)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun publishQueueError(event: QueueErrorEvent) {
        save(
            QueueSseOutboxEventType.QUEUE_ERROR,
            event.scheduleId,
            event.userId,
            event,
            LocalDateTime.now().plus(properties.terminalEventTtl),
        )
    }

    private fun save(
        eventType: QueueSseOutboxEventType,
        scheduleId: Long,
        userId: Long?,
        payload: Any,
        expiresAt: LocalDateTime,
    ) {
        repository.save(
            QueueSseOutboxEvent.create(
                eventType = eventType,
                scheduleId = scheduleId,
                userId = userId,
                payload = payloadCodec.encode(payload),
                expiresAt = expiresAt,
            ),
        )
    }
}
