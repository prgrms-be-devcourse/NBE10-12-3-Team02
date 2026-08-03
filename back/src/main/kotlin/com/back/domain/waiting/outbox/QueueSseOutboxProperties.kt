package com.back.domain.waiting.outbox

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("queue.sse.outbox")
data class QueueSseOutboxProperties(
    val batchSize: Int,
    val maxRetries: Int,
    val retryDelay: Duration,
    val processingTimeout: Duration,
    val terminalEventTtl: Duration,
    val retention: Duration,
)
