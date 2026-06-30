package com.back.domain.schedule.dto;

import com.back.domain.schedule.entity.Schedule;

import java.time.LocalDateTime;

public record ShowScheduleResponse(
            Long concertId,
            Long scheduleId,
            int round,
            LocalDateTime scheduleDate,
            long remainingSeats
) {
    public static ShowScheduleResponse from(Schedule schedule,long remainingSeats) {
        return new ShowScheduleResponse(
                schedule.getConcert().getConcertId(),
                schedule.getScheduleId(),
                schedule.getRound(),
                schedule.getScheduleDate(),
                remainingSeats
        );

    }
}
