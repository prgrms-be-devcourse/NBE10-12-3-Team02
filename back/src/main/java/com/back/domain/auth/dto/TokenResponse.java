package com.back.domain.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}