package com.back.domain.waiting.dto;

public record WaitingQueueResponse(
        Long concertId,
        Long scheduleId,
        Long userId,
        Long rank,
        Long myQueueNumber,
        String entryToken
) {
    public static WaitingQueueResponse of(Long concertId, Long scheduleId, Long userId, Long rank, Long myQueueNumber, String entryToken) {
        return new WaitingQueueResponse(concertId, scheduleId, userId, rank, myQueueNumber, entryToken);
    }
}
