package com.back.domain.waiting.outbox.service

import com.back.domain.waiting.outbox.constant.QueueSseOutboxStatus
import com.back.domain.waiting.outbox.repository.QueueSseOutboxRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class QueueSseOutboxDispatcherTest {
    @Test
    @DisplayName("PENDING 이벤트 claim에 성공하면 Outbox를 처리한다")
    fun t1() {
        val repository = repositoryReturningClaimResult(1)
        val processor = mock(QueueSseOutboxProcessor::class.java)
        val dispatcher = QueueSseOutboxDispatcher(repository, processor)

        dispatcher.dispatch(EVENT_ID)

        verify(processor).processClaimedEvent(EVENT_ID)
    }

    @Test
    @DisplayName("다른 처리자가 먼저 claim한 이벤트는 중복 처리하지 않는다")
    fun t2() {
        val repository = repositoryReturningClaimResult(0)
        val processor = mock(QueueSseOutboxProcessor::class.java)
        val dispatcher = QueueSseOutboxDispatcher(repository, processor)

        dispatcher.dispatch(EVENT_ID)

        verify(processor, never()).processClaimedEvent(EVENT_ID)
    }

    companion object {
        private const val EVENT_ID = "event-id"
    }

    private fun repositoryReturningClaimResult(claimResult: Int): QueueSseOutboxRepository =
        mock(QueueSseOutboxRepository::class.java) { invocation ->
            if (invocation.method.name == "claim") claimResult
            else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
}
