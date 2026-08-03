package com.back.domain.waiting.outbox.repository

import com.back.domain.waiting.outbox.constant.QueueSseOutboxStatus
import com.back.domain.waiting.outbox.entity.QueueSseOutboxEvent
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface QueueSseOutboxRepository : JpaRepository<QueueSseOutboxEvent, Long> {
    fun findByEventId(eventId: String): QueueSseOutboxEvent?

    @Query(
        """
        select event.eventId
        from QueueSseOutboxEvent event
        where event.status = :status
          and event.nextRetryAt <= :now
        order by event.createDate asc
        """,
    )
    fun findReadyEventIds(
        @Param("status") status: QueueSseOutboxStatus,
        @Param("now") now: LocalDateTime,
        pageable: Pageable,
    ): List<String>

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update QueueSseOutboxEvent event
        set event.status = :processingStatus,
            event.claimedAt = :claimedAt
        where event.eventId = :eventId
          and event.status = :pendingStatus
          and event.nextRetryAt <= :claimedAt
        """,
    )
    fun claim(
        @Param("eventId") eventId: String,
        @Param("pendingStatus") pendingStatus: QueueSseOutboxStatus,
        @Param("processingStatus") processingStatus: QueueSseOutboxStatus,
        @Param("claimedAt") claimedAt: LocalDateTime,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update QueueSseOutboxEvent event
        set event.status = :pendingStatus,
            event.claimedAt = null,
            event.nextRetryAt = :now
        where event.status = :processingStatus
          and event.claimedAt < :staleBefore
        """,
    )
    fun requeueStaleProcessingEvents(
        @Param("processingStatus") processingStatus: QueueSseOutboxStatus,
        @Param("pendingStatus") pendingStatus: QueueSseOutboxStatus,
        @Param("staleBefore") staleBefore: LocalDateTime,
        @Param("now") now: LocalDateTime,
    ): Int

    @Transactional
    fun deleteByStatusInAndProcessedAtBefore(
        statuses: Collection<QueueSseOutboxStatus>,
        processedAt: LocalDateTime,
    ): Long
}
