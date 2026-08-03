package com.back.domain.waiting.dto

data class QueueConnectionEvent(
    val concertId: Long,
    val scheduleId: Long,
    val userId: Long,
    val state: QueueConnectionState,
    val rank: Long,
    val myQueueNumber: Long,
    val entryToken: String?,
)
