package com.back.domain.waiting.outbox.scheduler

import com.back.domain.waiting.outbox.config.QueueSseOutboxProperties
import com.back.domain.waiting.outbox.repository.QueueSseOutboxRepository
import com.back.domain.waiting.outbox.service.QueueSseOutboxDispatcher
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.Duration

class QueueSseOutboxSchedulerTest {
    private val dispatcher = mock(QueueSseOutboxDispatcher::class.java)
    private val properties = QueueSseOutboxProperties(
        batchSize = 100,
        maxRetries = 3,
        retryDelay = Duration.ofSeconds(5),
        processingTimeout = Duration.ofSeconds(30),
        terminalEventTtl = Duration.ofMinutes(10),
        retention = Duration.ofDays(1),
    )

    @Test
    @DisplayName("즉시 처리되지 않고 남은 PENDING 이벤트를 복구 주기에 dispatcher로 전달한다")
    fun t1() {
        val repository = mock(QueueSseOutboxRepository::class.java) { invocation ->
            if (invocation.method.name == "findReadyEventIds") listOf(EVENT_ID)
            else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        val scheduler = QueueSseOutboxScheduler(repository, dispatcher, properties)

        scheduler.recoverPendingEvents()

        verify(dispatcher).dispatch(EVENT_ID)
    }

    companion object {
        private const val EVENT_ID = "event-id"
    }
}
