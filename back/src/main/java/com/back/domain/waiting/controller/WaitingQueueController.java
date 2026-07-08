package com.back.domain.waiting.controller;

import com.back.domain.waiting.dto.WaitingQueueResponse;
import com.back.domain.waiting.service.WaitingQueueService;
import com.back.global.annotation.ApiV1;
import com.back.global.requestcontext.RequestContext;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@ApiV1
@RestController
@RequestMapping("/waiting")
@RequiredArgsConstructor
@Tag(name = "Waiting", description = "Waiting API")
public class WaitingQueueController {
    private final WaitingQueueService waitingQueueService;
    private final RequestContext requestContext;

    @PostMapping("/concerts/{concertId}/schedules/{scheduleId}/waiting-queue")
    @Operation(summary = "대기열 등록", description = "대기열 등록 API")
    public RsData<WaitingQueueResponse> registerWaiting(
            @PathVariable Long concertId,
            @PathVariable Long scheduleId
    ) {
        WaitingQueueResponse response = waitingQueueService.registerWaiting(
                concertId,
                scheduleId,
                requestContext.getActor().getId()
        );
        return new RsData<>("200-1","대기열 등록 성공",response);
    }

    @GetMapping("/concerts/{concertId}/schedules/{scheduleId}/waiting-queue/rank")
    @Operation(summary = "대기열 순번 조회", description = "대기열 순번 조회 API")
    public RsData<WaitingQueueResponse> showWaitingRank(
            @PathVariable Long concertId,
            @PathVariable Long scheduleId
    ) {
        WaitingQueueResponse response = waitingQueueService.showWaitingRank(
                concertId,
                scheduleId,
                requestContext.getActor().getId()
        );

        return new RsData<>("200-1", "대기열 순번 조회 성공", response);
    }
    @DeleteMapping("/concerts/{concertId}/schedules/{scheduleId}/waiting-queue")
    @Operation(summary = "대기열 취소", description = "대기열 취소 API")
    public RsData<Void> cancelWaiting(
            @PathVariable Long concertId,
            @PathVariable Long scheduleId
    ) {
        waitingQueueService.cancelWaiting(
                concertId,
                scheduleId,
                requestContext.getActor().getId()
        );

        return new RsData<>("200-1", "대기열 취소 성공");
    }
}
