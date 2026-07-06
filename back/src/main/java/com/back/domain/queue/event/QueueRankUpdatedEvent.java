package com.back.domain.queue.event;

import java.time.LocalDateTime;

public record QueueRankUpdatedEvent(
        Long scheduleId,
        Long userId,
        Long currentRank,
        Long totalWaitingCount,
        LocalDateTime updatedAt
) {
    public static QueueRankUpdatedEvent of(Long scheduleId, Long userId, Long currentRank, Long totalWaitingCount) {
        return new QueueRankUpdatedEvent(scheduleId, userId, currentRank, totalWaitingCount, LocalDateTime.now());
    }
}
