package com.back.domain.waiting.outbox

import com.back.domain.queue.event.EntryAllowedEvent
import com.back.domain.queue.event.QueueErrorEvent
import com.back.domain.waiting.outbox.codec.QueueSseOutboxPayloadCodec
import com.back.domain.waiting.outbox.constant.QueueSseOutboxEventType
import com.back.domain.waiting.outbox.constant.QueueSseOutboxStatus
import com.back.domain.waiting.outbox.entity.QueueSseOutboxEvent
import com.back.domain.waiting.outbox.repository.QueueSseOutboxRepository
import com.back.domain.waiting.outbox.service.QueueSseOutboxProcessor
import com.back.domain.waiting.outbox.service.QueueSseOutboxPublisher
import com.back.domain.waiting.sse.QueueSseEmitterRegistry
import com.back.domain.waiting.sse.QueueSseDeliveryResult
import com.back.global.RedisTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest(properties = ["queue.sse.outbox.enabled=false"])
@Import(RedisTestConfig::class)
class QueueSseOutboxIntegrationTest {
    @Autowired
    private lateinit var publisher: QueueSseOutboxPublisher

    @Autowired
    private lateinit var processor: QueueSseOutboxProcessor

    @Autowired
    private lateinit var repository: QueueSseOutboxRepository

    @Autowired
    private lateinit var payloadCodec: QueueSseOutboxPayloadCodec

    @MockitoBean
    private lateinit var registry: QueueSseEmitterRegistry

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        reset(registry)
    }

    @Test
    @DisplayName("입장 허용 이벤트는 PENDING 상태로 Outbox에 저장된다")
    fun t1() {
        val event = entryAllowedEvent()

        publisher.publishEntryAllowed(event)

        val saved = repository.findAll().single()
        assertThat(saved.eventType).isEqualTo(QueueSseOutboxEventType.ENTRY_ALLOWED)
        assertThat(saved.status).isEqualTo(QueueSseOutboxStatus.PENDING)
        assertThat(saved.scheduleId).isEqualTo(SCHEDULE_ID)
        assertThat(saved.userId).isEqualTo(USER_ID)
        assertThat(payloadCodec.decode(saved.payload, EntryAllowedEvent::class.java)).isEqualTo(event)
    }

    @Test
    @DisplayName("동일 Outbox 이벤트는 하나의 처리자만 claim하고 처리 후 COMPLETED가 된다")
    fun t2() {
        val event = entryAllowedEvent()
        doReturn(QueueSseDeliveryResult.DELIVERED).`when`(registry).sendEntryAllowed(event)
        publisher.publishEntryAllowed(event)
        val eventId = readyEventIds().single()

        val firstClaim = claim(eventId)
        val secondClaim = claim(eventId)
        processor.processClaimedEvent(eventId)

        assertThat(firstClaim).isEqualTo(1)
        assertThat(secondClaim).isZero()
        assertThat(repository.findByEventId(eventId)?.status).isEqualTo(QueueSseOutboxStatus.COMPLETED)
        verify(registry).sendEntryAllowed(event)
    }

    @Test
    @DisplayName("이벤트 payload 처리 실패 시 재시도 횟수를 증가시키고 PENDING으로 되돌린다")
    fun t3() {
        val saved = repository.saveAndFlush(
            QueueSseOutboxEvent.create(
                eventType = QueueSseOutboxEventType.ENTRY_ALLOWED,
                scheduleId = SCHEDULE_ID,
                userId = USER_ID,
                payload = "invalid-json",
                expiresAt = LocalDateTime.now().plusMinutes(10),
            ),
        )
        claim(saved.eventId)

        processor.processClaimedEvent(saved.eventId)

        val failed = repository.findByEventId(saved.eventId)!!
        assertThat(failed.status).isEqualTo(QueueSseOutboxStatus.PENDING)
        assertThat(failed.retryCount).isEqualTo(1)
        assertThat(failed.lastError).isNotBlank()
        assertThat(failed.nextRetryAt).isAfter(LocalDateTime.now())
        verifyNoInteractions(registry)
    }

    @Test
    @DisplayName("유효시간이 지난 이벤트는 전송하지 않고 EXPIRED로 처리한다")
    fun t4() {
        val saved = repository.saveAndFlush(
            QueueSseOutboxEvent.create(
                eventType = QueueSseOutboxEventType.QUEUE_ERROR,
                scheduleId = SCHEDULE_ID,
                userId = null,
                payload = payloadCodec.encode(QueueErrorEvent(SCHEDULE_ID, null, "종료")),
                expiresAt = LocalDateTime.now().minusSeconds(1),
            ),
        )
        claim(saved.eventId)

        processor.processClaimedEvent(saved.eventId)

        assertThat(repository.findByEventId(saved.eventId)?.status).isEqualTo(QueueSseOutboxStatus.EXPIRED)
        verifyNoInteractions(registry)
    }

    @Test
    @DisplayName("처리 중 중단된 오래된 이벤트는 다시 PENDING으로 복구한다")
    fun t5() {
        publisher.publishEntryAllowed(entryAllowedEvent())
        val eventId = readyEventIds().single()
        claim(eventId)

        val restored = repository.requeueStaleProcessingEvents(
            QueueSseOutboxStatus.PROCESSING,
            QueueSseOutboxStatus.PENDING,
            LocalDateTime.now().plusSeconds(30),
            LocalDateTime.now(),
        )

        assertThat(restored).isEqualTo(1)
        assertThat(repository.findByEventId(eventId)?.status).isEqualTo(QueueSseOutboxStatus.PENDING)
    }

    @Test
    @DisplayName("회차 종료 오류 이벤트도 Outbox를 거쳐 SSE 레지스트리로 전달된다")
    fun t6() {
        val event = QueueErrorEvent(SCHEDULE_ID, null, "콘서트가 매진되었습니다.")
        doReturn(QueueSseDeliveryResult.DELIVERED).`when`(registry).sendError(event)
        publisher.publishQueueError(event)
        val eventId = readyEventIds().single()
        claim(eventId)

        processor.processClaimedEvent(eventId)

        assertThat(repository.findByEventId(eventId)?.status).isEqualTo(QueueSseOutboxStatus.COMPLETED)
        verify(registry).sendError(event)
    }

    @Test
    @DisplayName("SSE 구독자가 없으면 재시도하지 않고 SKIPPED로 기록한다")
    fun t7() {
        val event = entryAllowedEvent()
        doReturn(QueueSseDeliveryResult.NO_SUBSCRIBER).`when`(registry).sendEntryAllowed(event)
        publisher.publishEntryAllowed(event)
        val eventId = readyEventIds().single()
        claim(eventId)

        processor.processClaimedEvent(eventId)

        val skipped = repository.findByEventId(eventId)!!
        assertThat(skipped.status).isEqualTo(QueueSseOutboxStatus.SKIPPED)
        assertThat(skipped.lastError).contains("not connected")
        assertThat(skipped.retryCount).isZero()
    }

    @Test
    @DisplayName("끊어진 SSE 연결로 전송하지 못하면 재연결 복구 대상으로 SKIPPED 처리한다")
    fun t8() {
        val event = entryAllowedEvent()
        doReturn(QueueSseDeliveryResult.FAILED).`when`(registry).sendEntryAllowed(event)
        publisher.publishEntryAllowed(event)
        val eventId = readyEventIds().single()
        claim(eventId)

        processor.processClaimedEvent(eventId)

        val skipped = repository.findByEventId(eventId)!!
        assertThat(skipped.status).isEqualTo(QueueSseOutboxStatus.SKIPPED)
        assertThat(skipped.lastError).contains("recover on reconnect")
        assertThat(skipped.retryCount).isZero()
    }

    private fun readyEventIds(): List<String> = repository.findReadyEventIds(
        QueueSseOutboxStatus.PENDING,
        LocalDateTime.now(),
        PageRequest.of(0, 100),
    )

    private fun claim(eventId: String): Int = repository.claim(
        eventId,
        QueueSseOutboxStatus.PENDING,
        QueueSseOutboxStatus.PROCESSING,
        LocalDateTime.now(),
    )

    private fun entryAllowedEvent() = EntryAllowedEvent(
        scheduleId = SCHEDULE_ID,
        userId = USER_ID,
        entryToken = "entry-token",
        expiredAt = System.currentTimeMillis() + 600_000L,
    )

    companion object {
        private const val SCHEDULE_ID = 10L
        private const val USER_ID = 101L
    }
}
