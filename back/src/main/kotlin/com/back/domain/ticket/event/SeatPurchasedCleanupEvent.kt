package com.back.domain.ticket.event

import com.back.domain.ticket.dto.SeatHoldInfo

data class SeatPurchasedCleanupEvent(
    val concertId: Long,
    val scheduleId: Long,
    val seatHolds: List<SeatHoldInfo>
)
