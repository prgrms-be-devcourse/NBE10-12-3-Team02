package com.back.domain.queue.event;

import java.time.LocalDateTime;

public record QueueStatusEvent(
        Long scheduleId,
        Long currentRank,
        Long totalWaitingCount,
        LocalDateTime updatedAt
) {
    public static QueueStatusEvent of(Long scheduleId, Long currentAllowedSequence, Long totalWaitingCount) {
        return new QueueStatusEvent(scheduleId, currentAllowedSequence, totalWaitingCount, LocalDateTime.now());
    }
}
