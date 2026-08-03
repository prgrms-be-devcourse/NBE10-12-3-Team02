package com.back.domain.waiting.outbox.config

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties("queue.sse.outbox")
data class QueueSseOutboxProperties(
    @field:Min(1)
    val batchSize: Int,
    @field:Min(1)
    val maxRetries: Int,
    val retryDelay: Duration,
    val processingTimeout: Duration,
    val terminalEventTtl: Duration,
    val retention: Duration,
) {
    @get:AssertTrue(message = "retryDelay must be positive")
    val isRetryDelayPositive: Boolean
        get() = retryDelay.isPositive()

    @get:AssertTrue(message = "processingTimeout must be positive")
    val isProcessingTimeoutPositive: Boolean
        get() = processingTimeout.isPositive()

    @get:AssertTrue(message = "terminalEventTtl must be positive")
    val isTerminalEventTtlPositive: Boolean
        get() = terminalEventTtl.isPositive()

    @get:AssertTrue(message = "retention must be positive")
    val isRetentionPositive: Boolean
        get() = retention.isPositive()
}
