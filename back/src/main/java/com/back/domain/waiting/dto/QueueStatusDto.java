package com.back.domain.waiting.dto;

public record QueueStatusDto(
        Long currentAllowedSequence,
        Long totalWaitingCount
) {
}