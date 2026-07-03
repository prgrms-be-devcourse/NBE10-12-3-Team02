package com.back.domain.concert.dto;

public record SeatOccupyRequest (
        Long concertId,
        Long scheduleId,
        String seatNumber
) {
}
