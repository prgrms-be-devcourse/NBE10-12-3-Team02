package com.back.domain.queue.dto;

import com.back.domain.queue.constant.QueueEventType;

public record QueueEventResponse<T>(
        QueueEventType eventType,
        T data
) {
    public static <T> QueueEventResponse<T> of(QueueEventType eventType, T data) {
        return new QueueEventResponse<>(eventType, data);
    }
}
