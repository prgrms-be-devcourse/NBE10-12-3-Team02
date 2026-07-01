package com.back.global.security.jwt.payload;

public record RefreshTokenPayload(
        Long userId,
        String jti
) {
}