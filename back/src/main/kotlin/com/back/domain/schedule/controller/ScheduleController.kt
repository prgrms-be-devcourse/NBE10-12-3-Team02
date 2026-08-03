package com.back.domain.schedule.controller

import com.back.domain.schedule.dto.ShowScheduleListResponse
import com.back.domain.schedule.dto.ShowScheduleResponse
import com.back.domain.schedule.service.ScheduleService
import com.back.global.annotation.ApiV1
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ApiV1
@RestController
@RequestMapping("/schedules")
@Tag(name = "Schedule", description = "Schedule API")
class ScheduleController(
    private val scheduleService: ScheduleService
) {
    @GetMapping
    @Operation(summary = "콘서트 전체 회차 조회", description = "콘서트별 전체 회차 조회 API")
    fun showScheduleList(
        @RequestParam(value = "concertId") concertId: Long
    ): RsData<List<ShowScheduleListResponse>> {
        val response = scheduleService.showScheduleList(concertId)
        return RsData("200-1", "콘서트 전체 회차 조회 성공", response)
    }

    @GetMapping("/{scheduleId}/seats/status")
    @Operation(summary = "특정 회차 좌석 실시간 현황 조회", description = "특정 회차 좌석 실시간 현황 조회 API (30초 주기 폴링 보완 지원)")
    fun showSchedule(
        @RequestParam(value = "concertId") concertId: Long,
        @PathVariable(value = "scheduleId") scheduleId: Long,
        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) ifNoneMatch: String?
    ): ResponseEntity<RsData<ShowScheduleResponse>> {
        val response = scheduleService.showSchedule(concertId, scheduleId)
        val eTag = "\"$concertId-$scheduleId-${response.remainingSeats}\""

        if (!ifNoneMatch.isNullOrBlank() && ifNoneMatch == eTag) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .eTag(eTag)
                .build()
        }

        return ResponseEntity.ok()
            .eTag(eTag)
            .body(RsData("200-1", "특정 회차 좌석 실시간 조회 성공", response))
    }
}
