package com.back.domain.auth.dto;

public record LoginResponse
(
        String accessToken,
        String tokenType,
        Number expiresIn
) {
    public static LoginResponse of(String accessToken, String tokenType, Number expiresIn) {
        return new LoginResponse(accessToken, tokenType, expiresIn);
    }
}
