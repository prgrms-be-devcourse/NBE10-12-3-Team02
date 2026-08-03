package com.back.domain.waiting.sse

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QueueSseHeartbeatScheduler(
    private val registry: QueueSseEmitterRegistry,
) {
    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
    fun sendHeartbeat() {
        registry.sendHeartbeat()
    }

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 20_000L
    }
}
