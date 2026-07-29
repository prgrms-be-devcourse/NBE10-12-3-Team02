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

    /**
     * 좌석 선점(HOLD) 시 실시간 SSE 브로드캐스트.
     * @TransactionalEventListener(AFTER_COMMIT): DB 커밋 완료 후 발송 보장.
     *
     * ⚠️ @Async + @TransactionalEventListener 조합은 AsyncConfigurer 없이 사용 시
     *   Spring이 트랜잭션 동기화 컨텍스트를 잃어 이벤트가 무시(drop)된다.
     *   SseEmitter.send()는 in-memory write이므로 async 불필요.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSeatOccupied(event: SeatOccupiedEvent) {
        log.debug("SSE 브로드캐스트 (선점): scheduleId={}, seat={}", event.scheduleId, event.seatNumber)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.HOLD.name)
    }

    /**
     * 좌석 선점 취소(AVAILABLE 복구) 시 실시간 SSE 브로드캐스트.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSeatReleased(event: SeatReleasedEvent) {
        log.debug("SSE 브로드캐스트 (선점 취소): scheduleId={}, seat={}", event.scheduleId, event.seatNumber)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
    }

    /**
     * 선점 TTL 만료(AVAILABLE 복구) 시 SSE 브로드캐스트.
     * SeatHoldExpiredProcessor 트랜잭션 커밋 후 발송.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSeatExpired(event: SeatExpiredEvent) {
        log.debug("SSE 브로드캐스트 (만료 복구): scheduleId={}, seat={}", event.scheduleId, event.seatNumber)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
    }

    @EventListener
    fun onPaymentCompleted(event: PaymentCompletedEvent) {
        log.debug("SSE 브로드캐스트 (결제 완료): scheduleId={}", event.scheduleId)
    }
}

