package com.back.domain.concert.controller;

import com.back.domain.concert.dto.SeatSelectionResponse;
import com.back.domain.concert.service.ConcertService;
import com.back.global.annotation.ApiV1;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiV1
@RestController
@RequestMapping("/concerts")
@RequiredArgsConstructor
@Tag(name = "Concert", description = "Concert API")
public class ConcertController {
    private final ConcertService concertService;

    @GetMapping("/{concertId}/schedules/{scheduleId}/seats")
    public RsData<SeatSelectionResponse> getSeatSelection(
            @PathVariable Long concertId,
            @PathVariable Long scheduleId) {

        SeatSelectionResponse rsData = concertService.getSeatSelection(concertId, scheduleId);

        return new RsData<>(
                "200-1",
                "좌석 선택 페이지 조회 성공",
                rsData
        );
    }
}
