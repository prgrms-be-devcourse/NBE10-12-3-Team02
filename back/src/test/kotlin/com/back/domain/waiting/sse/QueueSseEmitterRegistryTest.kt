package com.back.domain.waiting.sse

import com.back.domain.queue.event.EntryAllowedEvent
import com.back.domain.queue.event.QueueErrorEvent
import com.back.domain.queue.event.QueueStatusEvent
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.LocalDateTime
import java.util.function.Consumer

class QueueSseEmitterRegistryTest {
    @Test
    @DisplayName("입장 허용 이벤트는 대상 사용자에게만 전송한다")
    fun t1() {
        val registry = QueueSseEmitterRegistry()
        val targetEmitter = mock(SseEmitter::class.java)
        val otherEmitter = mock(SseEmitter::class.java)
        registry.register(SCHEDULE_ID, TARGET_USER_ID, targetEmitter)
        registry.register(SCHEDULE_ID, OTHER_USER_ID, otherEmitter)

        val event = EntryAllowedEvent(SCHEDULE_ID, TARGET_USER_ID, "entry-token", 1000L)
        registry.sendEntryAllowed(event)
        registry.sendEntryAllowed(event)

        verify(targetEmitter, times(1)).send(any(SseEmitter.SseEventBuilder::class.java))
        verify(targetEmitter).complete()
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

    @Test
    @DisplayName("회차 전체 오류는 해당 회차 구독자에게만 전송한다")
    fun t4() {
        val registry = QueueSseEmitterRegistry()
        val targetScheduleEmitter = mock(SseEmitter::class.java)
        val otherScheduleEmitter = mock(SseEmitter::class.java)
        registry.register(SCHEDULE_ID, TARGET_USER_ID, targetScheduleEmitter)
        registry.register(OTHER_SCHEDULE_ID, OTHER_USER_ID, otherScheduleEmitter)

        val event = QueueErrorEvent(SCHEDULE_ID, null, "대기열 종료")
        registry.sendError(event)
        registry.sendError(event)

        verify(targetScheduleEmitter, times(1)).send(any(SseEmitter.SseEventBuilder::class.java))
        verify(targetScheduleEmitter).complete()
        verify(otherScheduleEmitter, never()).send(any(SseEmitter.SseEventBuilder::class.java))
    }

    @Test
    @DisplayName("heartbeat는 모든 회차의 구독자에게 전송한다")
    fun t5() {
        val registry = QueueSseEmitterRegistry()
        val firstEmitter = mock(SseEmitter::class.java)
        val secondEmitter = mock(SseEmitter::class.java)
        registry.register(SCHEDULE_ID, TARGET_USER_ID, firstEmitter)
        registry.register(OTHER_SCHEDULE_ID, OTHER_USER_ID, secondEmitter)

        registry.sendHeartbeat()

        verify(firstEmitter).send(any(SseEmitter.SseEventBuilder::class.java))
        verify(secondEmitter).send(any(SseEmitter.SseEventBuilder::class.java))
    }

    @Test
    @DisplayName("개인 오류 이벤트는 전송 후에도 SSE 연결을 유지한다")
    fun t6() {
        val registry = QueueSseEmitterRegistry()
        val emitter = mock(SseEmitter::class.java)
        registry.register(SCHEDULE_ID, TARGET_USER_ID, emitter)

        registry.sendError(QueueErrorEvent(SCHEDULE_ID, TARGET_USER_ID, "일시적 오류"))
        registry.sendHeartbeat()

        verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder::class.java))
        verify(emitter, never()).complete()
    }

    @Test
    @DisplayName("SSE 연결 완료 콜백이 실행되면 emitter를 레지스트리에서 제거한다")
    fun t7() {
        val registry = QueueSseEmitterRegistry()
        val emitter = mock(SseEmitter::class.java)
        val completionCaptor = ArgumentCaptor.forClass(Runnable::class.java)
        registry.register(SCHEDULE_ID, TARGET_USER_ID, emitter)
        verify(emitter).onCompletion(completionCaptor.capture())

        completionCaptor.value.run()
        registry.sendHeartbeat()

        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder::class.java))
    }

    @Test
    @DisplayName("SSE timeout 콜백이 실행되면 emitter를 레지스트리에서 제거한다")
    fun t8() {
        val registry = QueueSseEmitterRegistry()
        val emitter = mock(SseEmitter::class.java)
        val timeoutCaptor = ArgumentCaptor.forClass(Runnable::class.java)
        registry.register(SCHEDULE_ID, TARGET_USER_ID, emitter)
        verify(emitter).onTimeout(timeoutCaptor.capture())

        timeoutCaptor.value.run()
        registry.sendHeartbeat()

        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder::class.java))
    }

    @Test
    @DisplayName("동일 사용자에게 등록된 여러 연결은 모두 최종 이벤트를 받고 제거된다")
    fun t9() {
        val registry = QueueSseEmitterRegistry()
        val firstEmitter = mock(SseEmitter::class.java)
        val secondEmitter = mock(SseEmitter::class.java)
        registry.register(SCHEDULE_ID, TARGET_USER_ID, firstEmitter)
        registry.register(SCHEDULE_ID, TARGET_USER_ID, secondEmitter)

        registry.sendEntryAllowed(
            EntryAllowedEvent(SCHEDULE_ID, TARGET_USER_ID, "entry-token", 1000L),
        )
        registry.sendHeartbeat()

        verify(firstEmitter, times(1)).send(any(SseEmitter.SseEventBuilder::class.java))
        verify(secondEmitter, times(1)).send(any(SseEmitter.SseEventBuilder::class.java))
        verify(firstEmitter).complete()
        verify(secondEmitter).complete()
    }

    @Test
    @DisplayName("SSE 오류 콜백이 실행되면 emitter를 레지스트리에서 제거한다")
    @Suppress("UNCHECKED_CAST")
    fun t10() {
        val registry = QueueSseEmitterRegistry()
        val emitter = mock(SseEmitter::class.java)
        val errorCaptor = ArgumentCaptor.forClass(Consumer::class.java) as ArgumentCaptor<Consumer<Throwable>>
        registry.register(SCHEDULE_ID, TARGET_USER_ID, emitter)
        verify(emitter).onError(errorCaptor.capture())

        errorCaptor.value.accept(IOException("disconnected"))
        registry.sendHeartbeat()

        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder::class.java))
    }

    companion object {
        private const val SCHEDULE_ID = 10L
        private const val OTHER_SCHEDULE_ID = 11L
        private const val TARGET_USER_ID = 101L
        private const val OTHER_USER_ID = 102L
    }
}
