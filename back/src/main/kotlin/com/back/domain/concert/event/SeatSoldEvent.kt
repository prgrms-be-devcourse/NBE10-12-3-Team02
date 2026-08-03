package com.back.domain.concert.event

import com.back.domain.schedule.constant.SeatStatus

data class SeatSoldEvent(
    override val concertId: Long,
    override val scheduleId: Long,
    override val seatNumber: String
) : SeatEvent {
    override val status: SeatStatus get() = SeatStatus.SOLD_OUT
}
