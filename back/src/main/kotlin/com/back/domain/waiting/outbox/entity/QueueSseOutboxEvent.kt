package com.back.domain.waiting.outbox.entity

import com.back.domain.waiting.outbox.constant.QueueSseOutboxEventType
import com.back.domain.waiting.outbox.constant.QueueSseOutboxStatus
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "queue_sse_outbox_event",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_queue_sse_outbox_event_id", columnNames = ["event_id"]),
    ],
    indexes = [
        Index(
            name = "idx_queue_sse_outbox_ready",
            columnList = "status,next_retry_at,create_date",
        ),
        Index(
            name = "idx_queue_sse_outbox_processing",
            columnList = "status,claimed_at",
        ),
    ],
)
class QueueSseOutboxEvent private constructor(
    @Column(name = "event_id", nullable = false, updatable = false, length = 36)
    val eventId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 30)
    val eventType: QueueSseOutboxEventType,

    @Column(name = "schedule_id", nullable = false, updatable = false)
    val scheduleId: Long,

    @Column(name = "user_id", updatable = false)
    val userId: Long?,

    @Lob
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(name = "expires_at", nullable = false, updatable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "next_retry_at", nullable = false)
    var nextRetryAt: LocalDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: QueueSseOutboxStatus = QueueSseOutboxStatus.PENDING
        protected set

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0
        protected set

    @Column(name = "claimed_at")
    var claimedAt: LocalDateTime? = null
        protected set

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null
        protected set

    @Lob
    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null
        protected set

    fun isExpired(now: LocalDateTime): Boolean = !expiresAt.isAfter(now)

    fun complete(now: LocalDateTime) {
        status = QueueSseOutboxStatus.COMPLETED
        processedAt = now
        claimedAt = null
        lastError = null
    }

    fun expire(now: LocalDateTime) {
        status = QueueSseOutboxStatus.EXPIRED
        processedAt = now
        claimedAt = null
    }

    fun skip(now: LocalDateTime, reason: String) {
        status = QueueSseOutboxStatus.SKIPPED
        processedAt = now
        claimedAt = null
        lastError = reason.take(MAX_ERROR_LENGTH)
    }

    fun recordFailure(
        exception: Exception,
        now: LocalDateTime,
        maxRetries: Int,
        retryDelay: Duration,
    ) {
        retryCount += 1
        claimedAt = null
        lastError = (exception.message ?: exception.javaClass.simpleName).take(MAX_ERROR_LENGTH)

        if (retryCount >= maxRetries) {
            status = QueueSseOutboxStatus.FAILED
            processedAt = now
            return
        }

        status = QueueSseOutboxStatus.PENDING
        nextRetryAt = now.plus(retryDelay.multipliedBy(retryCount.toLong()))
    }

    companion object {
        private const val MAX_ERROR_LENGTH = 2000

        fun create(
            eventType: QueueSseOutboxEventType,
            scheduleId: Long,
            userId: Long?,
            payload: String,
            expiresAt: LocalDateTime,
            now: LocalDateTime = LocalDateTime.now(),
        ): QueueSseOutboxEvent = QueueSseOutboxEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = eventType,
            scheduleId = scheduleId,
            userId = userId,
            payload = payload,
            expiresAt = expiresAt,
            nextRetryAt = now,
        )
    }
}
