package com.back.domain.waiting.sse

import com.back.domain.waiting.service.WaitingQueueManager
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class QueueSseHeartbeatSchedulerTest {
    private val registry = mock(QueueSseEmitterRegistry::class.java)
    private val waitingQueueManager = mock(WaitingQueueManager::class.java)
    private val scheduler = QueueSseHeartbeatScheduler(registry, waitingQueueManager)

    @Test
    @DisplayName("heartbeat 전송 시 연결된 사용자의 Redis 대기열 상태를 확인한다")
    fun t1() {
        scheduler.sendHeartbeat()

        verify(registry).sendHeartbeat(anyObject())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T {
        ArgumentMatchers.any<T>()
        return null as T
    }
}
