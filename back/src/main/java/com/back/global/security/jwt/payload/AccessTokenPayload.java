package com.back.global.security.jwt.payload;

public record AccessTokenPayload(
        Long userId,
        String name
) {
}