package com.back.domain.waiting.dto

import com.back.domain.waiting.constant.QueueConnectionState

data class QueueConnectionEvent(
    val concertId: Long,
    val scheduleId: Long,
    val userId: Long,
    val state: QueueConnectionState,
    val rank: Long,
    val myQueueNumber: Long,
    val entryToken: String?,
    val errorCode: String? = null,
    val errorMessage: String? = null,
)
