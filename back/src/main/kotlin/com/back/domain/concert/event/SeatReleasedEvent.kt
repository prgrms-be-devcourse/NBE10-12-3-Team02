package com.back.domain.concert.event

data class SeatReleasedEvent(
    val concertId: Long,
    val scheduleId: Long,
    val seatNumber: String
)
