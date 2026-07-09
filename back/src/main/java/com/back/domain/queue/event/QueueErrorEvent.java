package com.back.domain.queue.event;

public record QueueErrorEvent(
        Long scheduleId,
        Long userId,
        String errorMessage
) {
}
