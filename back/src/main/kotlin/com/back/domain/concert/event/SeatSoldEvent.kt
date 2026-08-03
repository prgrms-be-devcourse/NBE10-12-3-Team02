package com.back.domain.concert.event

data class SeatSoldEvent(
    val concertId: Long,
    val scheduleId: Long,
    val seatNumber: String
)
