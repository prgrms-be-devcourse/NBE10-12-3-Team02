package com.back.domain.user.dto

import com.back.domain.ticket.entity.Ticket
import com.fasterxml.jackson.annotation.JsonProperty

data class TicketSummary(
    val ticketId: Long,
    val ticketNumber: String,
    val groupToken: String?,
    val seatNumber: String,
    val gradeName: String,
    val ticketPrice: Int,
    @get:JsonProperty("isValid") val isValid: Boolean,
    val createdAt: String
) {
    companion object {
        fun from(ticket: Ticket): TicketSummary = TicketSummary(
            ticketId = checkNotNull(ticket.ticketId) { "Ticket ID null" },
            ticketNumber = ticket.ticketNumber,
            groupToken = ticket.groupToken,
            seatNumber = ticket.scheduleSeat.seatNumber,
            gradeName = ticket.scheduleSeat.gradeName,
            ticketPrice = ticket.ticketPrice,
            isValid = ticket.isValid,
            createdAt = checkNotNull(ticket.createDate) { "Create date null" }.toLocalDate().toString()
        )
    }
}
