package com.back.domain.concert.dto;

import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.entity.SeatStatus;

import java.util.List;
import java.util.Map;

public record SeatSelectionResponse(
        Long concertId,
        Long scheduleId,
        Map<String, Integer> prices,
        List<SeatDetailResponse> seats
) {
    public static SeatSelectionResponse of(
            Long concertId,
            Long scheduleId,
            Map<String, Integer> prices,
            List<SeatDetailResponse> seats
    ){
        return new SeatSelectionResponse(
                concertId,
                scheduleId,
                prices,
                seats
        );
    }
    public record SeatDetailResponse(
            String seatNumber,
            SeatStatus seatStatus,
            String gradeName
    ) {
        public static SeatDetailResponse from(ScheduleSeat scheduleSeat){
            return new SeatDetailResponse(
                    scheduleSeat.getSeatNumber(),
                    scheduleSeat.getSeatStatus(),
                    scheduleSeat.getGradeName()
            );
        }
    }
}
