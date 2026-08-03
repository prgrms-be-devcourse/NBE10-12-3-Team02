package com.back.domain.concert.sse.repository

import com.back.domain.concert.sse.entity.SseOutboxEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SseOutboxEventRepository : JpaRepository<SseOutboxEvent, Long> {
    fun findByEventId(eventId: String): SseOutboxEvent?

    fun findByScheduleIdAndIdGreaterThanOrderByIdAsc(scheduleId: Long, id: Long): List<SseOutboxEvent>

    fun findByScheduleIdOrderByIdAsc(scheduleId: Long): List<SseOutboxEvent>

    @Modifying
    @Query("DELETE FROM SseOutboxEvent e WHERE e.createDate < :limitDate")
    fun deleteByCreateDateBefore(limitDate: LocalDateTime): Int
}
