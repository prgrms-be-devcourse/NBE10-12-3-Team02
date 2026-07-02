package com.back.domain.schedule.dto;

import com.back.domain.schedule.entity.Schedule;
import java.time.LocalDateTime;

public record ShowScheduleListResponse(
        Long scheduleId,
        int round,
        LocalDateTime scheduleDate,
        long remainingSeats
) {
    public static ShowScheduleListResponse of(Schedule schedule, long remainingSeats) {
        return new ShowScheduleListResponse(
                schedule.getScheduleId(),
                schedule.getRound(),
                schedule.getScheduleDate(),
                remainingSeats
        );
    }
}