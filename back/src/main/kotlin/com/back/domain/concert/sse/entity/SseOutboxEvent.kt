package com.back.domain.concert.sse.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "sse_outbox_event",
    indexes = [
        Index(name = "idx_outbox_schedule_id", columnList = "schedule_id"),
        Index(name = "idx_outbox_created_at", columnList = "create_date")
    ]
)
class SseOutboxEvent(
    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: String,

    @Column(name = "schedule_id", nullable = false)
    val scheduleId: Long,

    @Column(name = "seat_number", nullable = false)
    val seatNumber: String,

    @Column(name = "status", nullable = false)
    val status: String,

    @Column(name = "create_date", nullable = false, updatable = false)
    val createDate: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}
