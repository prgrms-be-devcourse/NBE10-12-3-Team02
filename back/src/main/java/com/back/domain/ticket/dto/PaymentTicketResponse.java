package com.back.domain.ticket.dto;

import com.back.domain.schedule.entity.SeatStatus;

import java.time.LocalDateTime;

public record PaymentTicketResponse(
        String ticketNumber,
        String urlPoster,
        String concertName,
        String seatNumber,
        LocalDateTime scheduleDate,
        SeatStatus seatStatus,
        boolean isValid
) {
}
