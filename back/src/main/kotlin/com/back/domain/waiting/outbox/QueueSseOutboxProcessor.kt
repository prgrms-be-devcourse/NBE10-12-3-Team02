package com.back.domain.waiting.outbox

import com.back.domain.queue.event.EntryAllowedEvent
import com.back.domain.queue.event.QueueErrorEvent
import com.back.domain.waiting.sse.QueueSseEmitterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class QueueSseOutboxProcessor(
    private val repository: QueueSseOutboxRepository,
    private val payloadCodec: QueueSseOutboxPayloadCodec,
    private val registry: QueueSseEmitterRegistry,
    private val properties: QueueSseOutboxProperties,
) {
    @Transactional
    fun processClaimedEvent(eventId: String) {
        val event = repository.findByEventId(eventId) ?: return
        if (event.status != QueueSseOutboxStatus.PROCESSING) return

        val now = LocalDateTime.now()
        if (event.isExpired(now)) {
            event.expire(now)
            return
        }

        try {
            when (event.eventType) {
                QueueSseOutboxEventType.ENTRY_ALLOWED -> registry.sendEntryAllowed(
                    payloadCodec.decode(event.payload, EntryAllowedEvent::class.java),
                )

                QueueSseOutboxEventType.QUEUE_ERROR -> registry.sendError(
                    payloadCodec.decode(event.payload, QueueErrorEvent::class.java),
                )
            }
            event.complete(now)
        } catch (e: Exception) {
            event.recordFailure(e, now, properties.maxRetries, properties.retryDelay)
            log.warn(
                "대기열 SSE Outbox 처리 실패: eventId={}, eventType={}, retryCount={}, error={}",
                event.eventId,
                event.eventType,
                event.retryCount,
                e.message,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(QueueSseOutboxProcessor::class.java)
    }
}
