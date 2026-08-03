package com.back.domain.waiting.outbox

enum class QueueSseOutboxStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED,
}
