package com.back.domain.ticket.event;

public record PaymentCompletedEvent(
        Long concertId,
        Long scheduleId,
        Long userId
) {
}
