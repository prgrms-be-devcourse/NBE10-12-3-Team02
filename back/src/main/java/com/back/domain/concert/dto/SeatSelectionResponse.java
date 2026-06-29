package com.back.domain.concert.dto;

import com.back.domain.schedule.entity.SeatStatus;

import java.util.List;
import java.util.Map;

public record SeatSelectionResponse(
        Long concertId,
        Long scheduleId,
        Map<String, Integer> prices,
        List<SeatDetailResponse> seats
) {
    public record SeatDetailResponse(
            String seatNumber,
            SeatStatus seatStatus,
            String gradeName
    ) {}
}
