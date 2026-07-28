package com.back.global.security.email

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("custom.auth.email-verification")
data class EmailVerificationProperties(
    @field:Positive
    val codeExpirationSeconds: Long,
    @field:Positive
    val verifiedExpirationSeconds: Long,
    @field:Positive
    val resendCooldownSeconds: Long,
    @field:Positive
    val maxAttempts: Long,
    @field:NotBlank
    val redisPrefix: String,
)
