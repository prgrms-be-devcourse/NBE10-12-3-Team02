package com.back.domain.waiting.event

data class EntryAllowedEvent(
    val scheduleId: Long,
    val userId: Long,
    val entryToken: String,
    val expiredAt: Long
)
