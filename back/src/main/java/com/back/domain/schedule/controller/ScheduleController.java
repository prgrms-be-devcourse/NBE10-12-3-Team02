package com.back.domain.schedule.controller;

import com.back.domain.schedule.dto.ShowScheduleResponse;
import com.back.domain.schedule.service.ScheduleService;
import com.back.global.annotation.ApiV1;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@ApiV1
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedule", description = "Schedule API")
public class ScheduleController {

    private final ScheduleService scheduleService;
    @GetMapping("/{scheduleId}/seats/status")
    @Operation(summary = "특정 회차 좌석 실시간 현황 조회", description = "특정 회차 좌석 실시간 현황 조회 API")
    public RsData<ShowScheduleResponse> showSchedule(
            @RequestParam(value = "concertId") Long concertId, @PathVariable(value = "scheduleId") Long scheduleId) {
        ShowScheduleResponse response = scheduleService.showSchedule(concertId, scheduleId);
        return new RsData<>(
                "200-1",
                "특정 회차 좌석 실시간 조회 성공",
                response
        );
    }
}
