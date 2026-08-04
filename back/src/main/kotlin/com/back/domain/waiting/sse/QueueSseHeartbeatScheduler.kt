package com.back.domain.waiting.sse

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QueueSseHeartbeatScheduler(
    private val registry: QueueSseEmitterRegistry,
) {
    @Scheduled(fixedDelayString = "\${queue.sse.heartbeat-interval}")
    fun sendHeartbeat() {
        registry.sendHeartbeat()
    }
}
