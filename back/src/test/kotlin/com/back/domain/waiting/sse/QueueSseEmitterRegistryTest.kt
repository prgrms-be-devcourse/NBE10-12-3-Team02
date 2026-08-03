package com.back.domain.waiting.sse

import com.back.domain.queue.event.EntryAllowedEvent
import com.back.domain.queue.event.QueueStatusEvent
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.LocalDateTime

class QueueSseEmitterRegistryTest {
    @Test
    @DisplayName("입장 허용 이벤트는 대상 사용자에게만 전송한다")
    fun t1() {
        val registry = QueueSseEmitterRegistry()
        val targetEmitter = mock(SseEmitter::class.java)
        val otherEmitter = mock(SseEmitter::class.java)
        registry.register(SCHEDULE_ID, TARGET_USER_ID, targetEmitter)
        registry.register(SCHEDULE_ID, OTHER_USER_ID, otherEmitter)

        registry.sendEntryAllowed(
            EntryAllowedEvent(SCHEDULE_ID, TARGET_USER_ID, "entry-token", 1000L),
        )

        verify(targetEmitter).send(any(SseEmitter.SseEventBuilder::class.java))
        verify(otherEmitter, never()).send(any(SseEmitter.SseEventBuilder::class.java))
    }

    @Test
    @DisplayName("대기열 상태 이벤트는 같은 회차의 구독자에게만 전송한다")
    fun t2() {
        val registry = QueueSseEmitterRegistry()
        val firstEmitter = mock(SseEmitter::class.java)
        val secondEmitter = mock(SseEmitter::class.java)
        val otherScheduleEmitter = mock(SseEmitter::class.java)
        registry.register(SCHEDULE_ID, TARGET_USER_ID, firstEmitter)
        registry.register(SCHEDULE_ID, OTHER_USER_ID, secondEmitter)
        registry.register(OTHER_SCHEDULE_ID, TARGET_USER_ID, otherScheduleEmitter)

        registry.broadcastStatus(
            SCHEDULE_ID,
            QueueStatusEvent(SCHEDULE_ID, 3L, 7L, LocalDateTime.now()),
        )

        verify(firstEmitter).send(any(SseEmitter.SseEventBuilder::class.java))
        verify(secondEmitter).send(any(SseEmitter.SseEventBuilder::class.java))
        verify(otherScheduleEmitter, never()).send(any(SseEmitter.SseEventBuilder::class.java))
    }

    @Test
    @DisplayName("전송에 실패한 SSE 연결은 제거하고 다른 연결 전송은 계속한다")
    fun t3() {
        val registry = QueueSseEmitterRegistry()
        val failedEmitter = mock(SseEmitter::class.java)
        val healthyEmitter = mock(SseEmitter::class.java)
        doThrow(IOException("disconnected"))
            .`when`(failedEmitter)
            .send(any(SseEmitter.SseEventBuilder::class.java))
        registry.register(SCHEDULE_ID, TARGET_USER_ID, failedEmitter)
        registry.register(SCHEDULE_ID, OTHER_USER_ID, healthyEmitter)
        val event = QueueStatusEvent(SCHEDULE_ID, 3L, 7L, LocalDateTime.now())

        registry.broadcastStatus(SCHEDULE_ID, event)
        registry.broadcastStatus(SCHEDULE_ID, event)

        verify(failedEmitter, times(1)).send(any(SseEmitter.SseEventBuilder::class.java))
        verify(healthyEmitter, times(2)).send(any(SseEmitter.SseEventBuilder::class.java))
    }

    companion object {
        private const val SCHEDULE_ID = 10L
        private const val OTHER_SCHEDULE_ID = 11L
        private const val TARGET_USER_ID = 101L
        private const val OTHER_USER_ID = 102L
    }
}
