package com.back.domain.waiting.outbox.config

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

class QueueSseOutboxPropertiesTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    @DisplayName("Outbox 설정값이 모두 유효하면 검증을 통과한다")
    fun t1() {
        val violations = validator.validate(validProperties())

        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("batchSize와 maxRetries는 1 이상이어야 한다")
    fun t2() {
        val properties = validProperties().copy(batchSize = 0, maxRetries = 0)

        val propertyPaths = validator.validate(properties).map { it.propertyPath.toString() }

        assertThat(propertyPaths).contains("batchSize", "maxRetries")
    }

    @Test
    @DisplayName("Outbox의 모든 시간 설정은 0보다 커야 한다")
    fun t3() {
        val properties = validProperties().copy(
            retryDelay = Duration.ZERO,
            processingTimeout = Duration.ofSeconds(-1),
            terminalEventTtl = Duration.ZERO,
            retention = Duration.ofDays(-1),
        )

        val messages = validator.validate(properties).map { it.message }

        assertThat(messages).containsExactlyInAnyOrder(
            "retryDelay must be positive",
            "processingTimeout must be positive",
            "terminalEventTtl must be positive",
            "retention must be positive",
        )
    }

    private fun validProperties() = QueueSseOutboxProperties(
        batchSize = 100,
        maxRetries = 3,
        retryDelay = Duration.ofSeconds(5),
        processingTimeout = Duration.ofSeconds(30),
        terminalEventTtl = Duration.ofMinutes(10),
        retention = Duration.ofDays(7),
    )
}
