package com.back.domain.waiting.service

sealed interface QueueConnectionSnapshot {
    data class Active(val entryToken: String) : QueueConnectionSnapshot

    data class Waiting(
        val rank: Long,
        val myQueueNumber: Long,
    ) : QueueConnectionSnapshot

    data object NotRegistered : QueueConnectionSnapshot
}

data class QueueAdmission(
    val userId: Long,
    val entryToken: String,
    val expiredAt: Long,
)
