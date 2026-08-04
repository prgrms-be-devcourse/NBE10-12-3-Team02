package com.back.domain.waiting.sse

import com.back.domain.waiting.service.WaitingQueueManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QueueSseHeartbeatScheduler(
    private val registry: QueueSseEmitterRegistry,
    private val waitingQueueManager: WaitingQueueManager,
) {
    @Scheduled(fixedDelayString = "\${queue.sse.heartbeat-interval}")
    fun sendHeartbeat() {
        registry.sendHeartbeat(waitingQueueManager::getConnectionSnapshot)
    }
}
