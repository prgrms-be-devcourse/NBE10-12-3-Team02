package com.back.domain.ticket.dto

import com.back.domain.ticket.entity.Ticket
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class TicketGroupVerifyResponse(
    val concertName: String,
    val venueName: String,
    val scheduleDate: LocalDateTime,
    val seats: List<SeatVerifyInfo>
) {
    data class SeatVerifyInfo(
        val seatNumber: String,
        @get:JsonProperty("isValid") val isValid: Boolean
    )

    companion object {
        fun from(tickets: List<Ticket>): TicketGroupVerifyResponse {
            val first = tickets.first()
            return TicketGroupVerifyResponse(
                concertName = first.schedule.concert.concertName,
                venueName = first.schedule.venue.venueName,
                scheduleDate = first.schedule.scheduleDate,
                seats = tickets
                    .sortedBy { it.scheduleSeat.seatNumber }
                    .map { SeatVerifyInfo(it.scheduleSeat.seatNumber, it.isValid) }
            )
        }
    }
}
