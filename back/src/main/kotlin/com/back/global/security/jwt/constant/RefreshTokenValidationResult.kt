package com.back.global.security.jwt.constant

enum class RefreshTokenValidationResult {
    SUCCESS,
    NOT_FOUND,
    MISMATCH,
    REUSED,
}
