package com.back.domain.concert.sse

import com.back.domain.concert.event.SeatExpiredEvent
import com.back.domain.concert.event.SeatOccupiedEvent
import com.back.domain.concert.event.SeatReleasedEvent
import com.back.domain.concert.event.SeatSoldEvent
import com.back.domain.concert.sse.service.SseOutboxService
import com.back.domain.schedule.constant.SeatStatus
import com.back.domain.ticket.event.PaymentCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SeatStatusSseBroadcaster(
    private val registry: SeatStatusSseEmitterRegistry,
    private val sseOutboxService: SseOutboxService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val lastGeneratedEventIdMapThreadLocal = ThreadLocal.withInitial { mutableMapOf<String, String>() }

    private fun getEventKey(scheduleId: Long, seatNumber: String): String = "$scheduleId:$seatNumber"

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatOccupied(event: SeatOccupiedEvent) {
        val outbox = sseOutboxService.saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.HOLD.name)
        if (outbox != null) {
            lastGeneratedEventIdMapThreadLocal.get()[getEventKey(event.scheduleId, event.seatNumber)] = outbox.eventId
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatReleased(event: SeatReleasedEvent) {
        val outbox = sseOutboxService.saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
        if (outbox != null) {
            lastGeneratedEventIdMapThreadLocal.get()[getEventKey(event.scheduleId, event.seatNumber)] = outbox.eventId
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatExpired(event: SeatExpiredEvent) {
        val outbox = sseOutboxService.saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
        if (outbox != null) {
            lastGeneratedEventIdMapThreadLocal.get()[getEventKey(event.scheduleId, event.seatNumber)] = outbox.eventId
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatSold(event: SeatSoldEvent) {
        val outbox = sseOutboxService.saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.SOLD_OUT.name)
        if (outbox != null) {
            lastGeneratedEventIdMapThreadLocal.get()[getEventKey(event.scheduleId, event.seatNumber)] = outbox.eventId
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatOccupied(event: SeatOccupiedEvent) {
        val map = lastGeneratedEventIdMapThreadLocal.get()
        val key = getEventKey(event.scheduleId, event.seatNumber)
        val eventId = map.remove(key)
        if (map.isEmpty()) lastGeneratedEventIdMapThreadLocal.remove()
        log.info("SSE 브로드캐스트 (선점): scheduleId={}, seat={}, eventId={}", event.scheduleId, event.seatNumber, eventId)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.HOLD.name, eventId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatReleased(event: SeatReleasedEvent) {
        val map = lastGeneratedEventIdMapThreadLocal.get()
        val key = getEventKey(event.scheduleId, event.seatNumber)
        val eventId = map.remove(key)
        if (map.isEmpty()) lastGeneratedEventIdMapThreadLocal.remove()
        log.info("SSE 브로드캐스트 (선점 취소): scheduleId={}, seat={}, eventId={}", event.scheduleId, event.seatNumber, eventId)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name, eventId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatExpired(event: SeatExpiredEvent) {
        val map = lastGeneratedEventIdMapThreadLocal.get()
        val key = getEventKey(event.scheduleId, event.seatNumber)
        val eventId = map.remove(key)
        if (map.isEmpty()) lastGeneratedEventIdMapThreadLocal.remove()
        log.info("SSE 브로드캐스트 (만료 복구): scheduleId={}, seat={}, eventId={}", event.scheduleId, event.seatNumber, eventId)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name, eventId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatSold(event: SeatSoldEvent) {
        val map = lastGeneratedEventIdMapThreadLocal.get()
        val key = getEventKey(event.scheduleId, event.seatNumber)
        val eventId = map.remove(key)
        if (map.isEmpty()) lastGeneratedEventIdMapThreadLocal.remove()
        log.info("SSE 브로드캐스트 (결제 판매완료): scheduleId={}, seat={}, eventId={}", event.scheduleId, event.seatNumber, eventId)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.SOLD_OUT.name, eventId)
    }

    @EventListener
    fun onPaymentCompleted(event: PaymentCompletedEvent) {
        log.debug("SSE 브로드캐스트 (결제 완료): scheduleId={}", event.scheduleId)
    }
}
