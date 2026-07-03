package com.back.domain.concert.controller;

import com.back.domain.concert.dto.*;
import com.back.domain.concert.enums.ConcertSortType;
import com.back.domain.concert.service.ConcertService;
import com.back.domain.concert.service.SeatOccupyManager;
import com.back.global.annotation.ApiV1;
import com.back.global.requestcontext.RequestContext;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@ApiV1
@RestController
@RequestMapping("/concerts")
@RequiredArgsConstructor
@Tag(name = "Concert", description = "Concert API")
public class ConcertController {
    private final ConcertService concertService;
    private final SeatOccupyManager seatOccupyManager;
    private final RequestContext requestContext;

    @GetMapping
    @Operation(summary = "콘서트 목록 조회", description = "콘서트 목록 조회 API")
    public RsData<List<ConcertListResponse>> getConcerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "closingSoon") ConcertSortType sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<ConcertListResponse> data = concertService.getConcerts(keyword, sort, date);
        return new RsData<>("200-1", "콘서트 목록 조회 성공", data);
    }

    @GetMapping("/{concertId}")
    @Operation(summary = "콘서트 상세 조회", description = "콘서트 상세 정보 조회 API")
    public RsData<ConcertDetailResponse> getConcertDetail(
            @PathVariable Long concertId) {

        ConcertDetailResponse data = concertService.getConcertDetail(concertId);
        return new RsData<>("200-1", "콘서트 상세 정보 조회 성공", data);
    }

    @GetMapping("/{concertId}/schedules/{scheduleId}/seats")
    @Operation(summary = "좌석 선택 페이지", description = "좌석 선택 페이지 API")
    public RsData<SeatSelectionResponse> getSeatSelection(
            @PathVariable Long concertId,
            @PathVariable Long scheduleId) {

        SeatSelectionResponse response = seatOccupyManager.getSeatSelection(concertId, scheduleId);

        return new RsData<>(
                "200-1",
                "좌석 선택 페이지 조회 성공",
                response
        );
    }

    @PostMapping("/seats/occupy")
    @Operation(summary = "Redis 실시간 좌석 선점 요청", description = "Redis 실시간 좌석 선점 요청 API")
    public RsData<SeatOccupyResponse> seatOccupy(
            @RequestBody SeatOccupyRequest request
    ) {
        SeatOccupyResponse response = seatOccupyManager.seatOccupy(
                request.concertId(),
                request.scheduleId(),
                request.seatNumber(),
                requestContext.getActor().getId()
        );

        return new RsData<>(
                "200-1",
                "좌석 임시 선점에 성공했습니다.",
                response
        );
    }

    @DeleteMapping("/seats/occupy")
    @Operation(summary = "Redis 실시간 좌석 선점 취소", description = "Redis 실시간 좌석 선점 취소 API")
    public RsData<Void> seatOccupyCancel(
            @RequestBody SeatOccupyRequest request
    ) {
        seatOccupyManager.seatOccupyCancel(
                request.concertId(),
                request.scheduleId(),
                request.seatNumber(),
                requestContext.getActor().getId()
        );

        return new RsData<>(
                "200-1",
                "좌석 선점이 정상적으로 취소되었습니다."
        );
    }
}