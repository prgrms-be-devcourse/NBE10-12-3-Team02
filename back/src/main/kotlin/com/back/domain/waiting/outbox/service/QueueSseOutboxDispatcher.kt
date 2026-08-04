package com.back.domain.waiting.outbox.service

import com.back.domain.waiting.outbox.constant.QueueSseOutboxStatus
import com.back.domain.waiting.outbox.repository.QueueSseOutboxRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class QueueSseOutboxDispatcher(
    private val repository: QueueSseOutboxRepository,
    private val processor: QueueSseOutboxProcessor,
) {
    fun dispatch(eventId: String) {
        val claimed = repository.claim(
            eventId = eventId,
            pendingStatus = QueueSseOutboxStatus.PENDING,
            processingStatus = QueueSseOutboxStatus.PROCESSING,
            claimedAt = LocalDateTime.now(),
        )

        if (claimed == 1) {
            processor.processClaimedEvent(eventId)
        }
    }
}
