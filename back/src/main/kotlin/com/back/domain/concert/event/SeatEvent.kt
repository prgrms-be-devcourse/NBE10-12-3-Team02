package com.back.domain.concert.event

import com.back.domain.schedule.constant.SeatStatus

sealed interface SeatEvent {
    val concertId: Long
    val scheduleId: Long
    val seatNumber: String
    val status: SeatStatus
}
