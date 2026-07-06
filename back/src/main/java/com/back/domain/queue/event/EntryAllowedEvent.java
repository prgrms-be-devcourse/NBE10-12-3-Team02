package com.back.domain.queue.event;

public record EntryAllowedEvent(
        Long scheduleId,
        Long userId,
        String entryToken,
        long expiredAt
) {
}
