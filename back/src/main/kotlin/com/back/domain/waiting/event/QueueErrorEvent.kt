package com.back.domain.waiting.event

data class QueueErrorEvent(
    val scheduleId: Long,
    val userId: Long?,
    val errorMessage: String
)
