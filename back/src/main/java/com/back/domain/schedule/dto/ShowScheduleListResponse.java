package com.back.domain.schedule.dto;

import com.back.domain.schedule.entity.Schedule;

import java.time.LocalDateTime;

public record ShowScheduleListResponse(
        Long scheduleId,
        int round,
        LocalDateTime scheduleDate
) {
    public static ShowScheduleListResponse of(Schedule schedule) {
        return new ShowScheduleListResponse(
                schedule.getScheduleId(),
                schedule.getRound(),
                schedule.getScheduleDate()
        );
    }
}
