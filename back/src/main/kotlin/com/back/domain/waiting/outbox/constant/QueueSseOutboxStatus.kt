package com.back.domain.waiting.outbox.constant

enum class QueueSseOutboxStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    SKIPPED,
    FAILED,
    EXPIRED,
}
