package com.back.domain.concert.sse

import com.back.domain.concert.event.SeatExpiredEvent
import com.back.domain.concert.event.SeatOccupiedEvent
import com.back.domain.concert.event.SeatReleasedEvent
import com.back.domain.schedule.constant.SeatStatus
import com.back.domain.ticket.event.PaymentCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SeatStatusSseBroadcaster(
    private val registry: SeatStatusSseEmitterRegistry
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatOccupied(event: SeatOccupiedEvent) {
        log.info("SSE 브로드캐스트 (선점): scheduleId={}, seat={}", event.scheduleId, event.seatNumber)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.HOLD.name)
    }

    /**
     * 좌석 선점 취소(AVAILABLE 복구) 시 실시간 SSE 브로드캐스트.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatReleased(event: SeatReleasedEvent) {
        log.info("SSE 브로드캐스트 (선점 취소): scheduleId={}, seat={}", event.scheduleId, event.seatNumber)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
    }

    /**
     * 선점 TTL 만료(AVAILABLE 복구) 시 SSE 브로드캐스트.
     * SeatHoldExpiredProcessor 트랜잭션 커밋 후 발송.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatExpired(event: SeatExpiredEvent) {
        log.info("SSE 브로드캐스트 (만료 복구): scheduleId={}, seat={}", event.scheduleId, event.seatNumber)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
    }

    @EventListener
    fun onPaymentCompleted(event: PaymentCompletedEvent) {
        log.debug("SSE 브로드캐스트 (결제 완료): scheduleId={}", event.scheduleId)
    }
}

