package com.back.domain.waiting.outbox.service

import com.back.domain.waiting.event.EntryAllowedEvent
import com.back.domain.waiting.event.QueueErrorEvent
import com.back.domain.waiting.outbox.codec.QueueSseOutboxPayloadCodec
import com.back.domain.waiting.outbox.constant.QueueSseOutboxEventType
import com.back.domain.waiting.sse.QueueSseEmitterRegistry
import com.back.domain.waiting.sse.QueueSseDeliveryResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class QueueSseOutboxProcessor(
    private val transactionService: QueueSseOutboxTransactionService,
    private val payloadCodec: QueueSseOutboxPayloadCodec,
    private val registry: QueueSseEmitterRegistry,
) {
    fun processClaimedEvent(eventId: String) {
        val event = transactionService.loadProcessingEvent(eventId) ?: return

        val now = LocalDateTime.now()
        if (event.isExpired(now)) {
            transactionService.markExpired(eventId, now)
            return
        }

        try {
            val deliveryResult = when (event.eventType) {
                QueueSseOutboxEventType.ENTRY_ALLOWED -> registry.sendEntryAllowed(
                    payloadCodec.decode(event.payload, EntryAllowedEvent::class.java),
                )

                QueueSseOutboxEventType.QUEUE_ERROR -> registry.sendError(
                    payloadCodec.decode(event.payload, QueueErrorEvent::class.java),
                )
            }
            when (deliveryResult) {
                QueueSseDeliveryResult.DELIVERED -> transactionService.markCompleted(eventId, now)
                QueueSseDeliveryResult.NO_SUBSCRIBER -> transactionService.markSkipped(
                    eventId,
                    now,
                    "SSE subscriber not connected",
                )
                QueueSseDeliveryResult.FAILED -> transactionService.markSkipped(
                    eventId,
                    now,
                    "SSE delivery failed; recover on reconnect",
                )
            }
        } catch (e: Exception) {
            val retryCount = transactionService.recordFailure(eventId, e, now)
            log.warn(
                "대기열 SSE Outbox 처리 실패: eventId={}, eventType={}, retryCount={}, error={}",
                event.eventId,
                event.eventType,
                retryCount,
                e.message,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(QueueSseOutboxProcessor::class.java)
    }
}
