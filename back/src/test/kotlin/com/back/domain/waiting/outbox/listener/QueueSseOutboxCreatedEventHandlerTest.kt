package com.back.domain.waiting.outbox.listener

import com.back.domain.waiting.outbox.event.QueueSseOutboxCreatedEvent
import com.back.domain.waiting.outbox.service.QueueSseOutboxDispatcher
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException

class QueueSseOutboxCreatedEventHandlerTest {
    private val dispatcher = mock(QueueSseOutboxDispatcher::class.java)

    @Test
    @DisplayName("Outbox 커밋 이벤트를 실행기에 제출하여 즉시 처리한다")
    fun t1() {
        val directExecutor = TaskExecutor { task -> task.run() }
        val handler = QueueSseOutboxCreatedEventHandler(dispatcher, directExecutor)

        handler.handle(QueueSseOutboxCreatedEvent(EVENT_ID))

        verify(dispatcher).dispatch(EVENT_ID)
    }

    @Test
    @DisplayName("실행기가 작업을 거부해도 예외를 전파하지 않고 복구 스케줄러에 맡긴다")
    fun t2() {
        val rejectingExecutor = TaskExecutor { throw TaskRejectedException("queue is full") }
        val handler = QueueSseOutboxCreatedEventHandler(dispatcher, rejectingExecutor)

        assertThatCode {
            handler.handle(QueueSseOutboxCreatedEvent(EVENT_ID))
        }.doesNotThrowAnyException()
        verifyNoInteractions(dispatcher)
    }

    companion object {
        private const val EVENT_ID = "event-id"
    }
}
