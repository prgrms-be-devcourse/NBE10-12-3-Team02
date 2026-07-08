package com.back.domain.waiting.dto;

public record ActiveEntry(
        String entryToken,
        long expiredAt
) {
}
