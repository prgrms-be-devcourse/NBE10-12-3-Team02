package com.back.domain.concert.sse

import com.back.domain.concert.event.SeatExpiredEvent
import com.back.domain.concert.event.SeatOccupiedEvent
import com.back.domain.concert.event.SeatReleasedEvent
import com.back.domain.concert.sse.entity.SseOutboxEvent
import com.back.domain.concert.sse.repository.SseOutboxEventRepository
import com.back.domain.schedule.constant.SeatStatus
import com.back.domain.ticket.event.PaymentCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

@Component
class SeatStatusSseBroadcaster(
    private val registry: SeatStatusSseEmitterRegistry,
    private val sseOutboxEventRepository: SseOutboxEventRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatOccupied(event: SeatOccupiedEvent) {
        saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.HOLD.name)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatReleased(event: SeatReleasedEvent) {
        saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatExpired(event: SeatExpiredEvent) {
        saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatOccupied(event: SeatOccupiedEvent) {
        log.info("SSE 브로드캐스트 (선점): scheduleId={}, seat={}", event.scheduleId, event.seatNumber)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.HOLD.name)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatReleased(event: SeatReleasedEvent) {
        log.info("SSE 브로드캐스트 (선점 취소): scheduleId={}, seat={}", event.scheduleId, event.seatNumber)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatExpired(event: SeatExpiredEvent) {
        log.info("SSE 브로드캐스트 (만료 복구): scheduleId={}, seat={}", event.scheduleId, event.seatNumber)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
    }

    @EventListener
    fun onPaymentCompleted(event: PaymentCompletedEvent) {
        log.debug("SSE 브로드캐스트 (결제 완료): scheduleId={}", event.scheduleId)
    }

    private fun saveOutboxEvent(scheduleId: Long, seatNumber: String, status: String) {
        try {
            val eventId = "$scheduleId:${UUID.randomUUID()}"
            sseOutboxEventRepository.save(SseOutboxEvent(eventId, scheduleId, seatNumber, status))
        } catch (e: Exception) {
            log.warn("SseOutboxEvent 저장 실패: scheduleId={}, seat={}", scheduleId, seatNumber, e)
        }
    }
}
