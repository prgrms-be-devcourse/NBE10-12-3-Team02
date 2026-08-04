package com.back.domain.waiting.outbox.event

data class QueueSseOutboxCreatedEvent(
    val eventId: String,
)
