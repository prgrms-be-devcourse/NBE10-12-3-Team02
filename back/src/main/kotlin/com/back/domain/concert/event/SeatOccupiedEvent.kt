package com.back.domain.concert.event

import com.back.domain.schedule.constant.SeatStatus

data class SeatOccupiedEvent(
    override val concertId: Long,
    override val scheduleId: Long,
    override val seatNumber: String,
    val ttlSeconds: Long
) : SeatEvent {
    override val status: SeatStatus get() = SeatStatus.HOLD
}
