package com.back.domain.waiting.outbox.service

import com.back.domain.waiting.event.EntryAllowedEvent
import com.back.domain.waiting.outbox.codec.QueueSseOutboxPayloadCodec
import com.back.domain.waiting.outbox.config.QueueSseOutboxProperties
import com.back.domain.waiting.outbox.entity.QueueSseOutboxEvent
import com.back.domain.waiting.outbox.event.QueueSseOutboxCreatedEvent
import com.back.domain.waiting.outbox.repository.QueueSseOutboxRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.time.Duration

class QueueSseOutboxPublisherTest {
    private val repository = mock(QueueSseOutboxRepository::class.java)
    private val payloadCodec = mock(QueueSseOutboxPayloadCodec::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val properties = QueueSseOutboxProperties(
        batchSize = 100,
        maxRetries = 3,
        retryDelay = Duration.ofSeconds(5),
        processingTimeout = Duration.ofSeconds(30),
        terminalEventTtl = Duration.ofMinutes(10),
        retention = Duration.ofDays(1),
    )
    private val publisher = QueueSseOutboxPublisher(
        repository,
        payloadCodec,
        properties,
        eventPublisher,
    )

    @Test
    @DisplayName("Outbox를 저장한 뒤 생성된 eventId로 즉시 처리 신호를 발행한다")
    fun t1() {
        val event = EntryAllowedEvent(
            scheduleId = 10L,
            userId = 101L,
            entryToken = "entry-token",
            expiredAt = System.currentTimeMillis() + 600_000L,
        )
        `when`(payloadCodec.encode(event)).thenReturn("payload")
        doAnswer { invocation -> invocation.arguments[0] }
            .`when`(repository).save(anyObject<QueueSseOutboxEvent>())

        publisher.publishEntryAllowed(event)

        val outboxCaptor = ArgumentCaptor.forClass(QueueSseOutboxEvent::class.java)
        val signalCaptor = ArgumentCaptor.forClass(QueueSseOutboxCreatedEvent::class.java)
        verify(repository).save(outboxCaptor.capture())
        verify(eventPublisher).publishEvent(signalCaptor.capture())
        assertThat(signalCaptor.value.eventId).isEqualTo(outboxCaptor.value.eventId)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T {
        ArgumentMatchers.any<T>()
        return null as T
    }
}
