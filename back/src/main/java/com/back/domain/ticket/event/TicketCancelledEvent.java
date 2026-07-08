package com.back.domain.ticket.event;

public record TicketCancelledEvent(
        Long concertId,
        Long scheduleId,
        Long userId
) {
}
