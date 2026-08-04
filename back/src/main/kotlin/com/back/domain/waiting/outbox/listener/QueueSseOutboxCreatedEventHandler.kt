package com.back.domain.waiting.outbox.listener

import com.back.domain.waiting.outbox.event.QueueSseOutboxCreatedEvent
import com.back.domain.waiting.outbox.service.QueueSseOutboxDispatcher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.RejectedExecutionException

@Component
@ConditionalOnProperty(
    prefix = "queue.sse.outbox",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class QueueSseOutboxCreatedEventHandler(
    private val dispatcher: QueueSseOutboxDispatcher,
    @Qualifier("applicationTaskExecutor")
    private val taskExecutor: TaskExecutor,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: QueueSseOutboxCreatedEvent) {
        try {
            taskExecutor.execute {
                runCatching {
                    dispatcher.dispatch(event.eventId)
                }.onFailure { exception ->
                    log.warn(
                        "대기열 SSE Outbox 즉시 처리 실패, 복구 스케줄러에서 재시도: eventId={}",
                        event.eventId,
                        exception,
                    )
                }
            }
        } catch (e: RejectedExecutionException) {
            // Outbox는 PENDING으로 커밋되어 있으므로 요청을 실패시키지 않고 복구 스케줄러에 맡긴다.
            log.warn(
                "대기열 SSE Outbox 작업 제출 실패, 복구 스케줄러에서 재시도: eventId={}",
                event.eventId,
                e,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(QueueSseOutboxCreatedEventHandler::class.java)
    }
}
