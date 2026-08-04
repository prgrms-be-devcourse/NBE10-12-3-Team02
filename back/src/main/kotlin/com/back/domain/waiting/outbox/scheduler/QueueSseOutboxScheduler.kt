package com.back.domain.waiting.outbox.scheduler

import com.back.domain.waiting.outbox.config.QueueSseOutboxProperties
import com.back.domain.waiting.outbox.constant.QueueSseOutboxStatus
import com.back.domain.waiting.outbox.repository.QueueSseOutboxRepository
import com.back.domain.waiting.outbox.service.QueueSseOutboxDispatcher
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@ConditionalOnProperty(
    prefix = "queue.sse.outbox",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class QueueSseOutboxScheduler(
    private val repository: QueueSseOutboxRepository,
    private val dispatcher: QueueSseOutboxDispatcher,
    private val properties: QueueSseOutboxProperties,
) {
    @Scheduled(fixedDelayString = "\${queue.sse.outbox.recovery-interval}")
    fun recoverPendingEvents() {
        val now = LocalDateTime.now()
        repository.requeueStaleProcessingEvents(
            processingStatus = QueueSseOutboxStatus.PROCESSING,
            pendingStatus = QueueSseOutboxStatus.PENDING,
            staleBefore = now.minus(properties.processingTimeout),
            now = now,
        )

        val eventIds = repository.findReadyEventIds(
            QueueSseOutboxStatus.PENDING,
            now,
            PageRequest.of(0, properties.batchSize),
        )

        eventIds.forEach { eventId ->
            try {
                dispatcher.dispatch(eventId)
            } catch (e: Exception) {
                log.error("대기열 SSE Outbox 스케줄 처리 실패: eventId={}, error={}", eventId, e.message, e)
            }
        }
    }

    @Scheduled(fixedDelayString = "\${queue.sse.outbox.cleanup-interval}")
    fun cleanupProcessedEvents() {
        repository.deleteByStatusInAndProcessedAtBefore(
            statuses = TERMINAL_STATUSES,
            processedAt = LocalDateTime.now().minus(properties.retention),
        )
    }

    companion object {
        private val TERMINAL_STATUSES = listOf(
            QueueSseOutboxStatus.COMPLETED,
            QueueSseOutboxStatus.SKIPPED,
            QueueSseOutboxStatus.FAILED,
            QueueSseOutboxStatus.EXPIRED,
        )
        private val log = LoggerFactory.getLogger(QueueSseOutboxScheduler::class.java)
    }
}
