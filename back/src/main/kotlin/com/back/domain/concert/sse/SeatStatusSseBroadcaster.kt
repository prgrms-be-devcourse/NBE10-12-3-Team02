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
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.ConcurrentHashMap

@Component
class SeatStatusSseBroadcaster(
    private val registry: SeatStatusSseEmitterRegistry,
    private val sseOutboxService: SseOutboxService,
    private val eTagVersionManager: SeatStatusETagVersionManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ──────────────────────────────────────────────────────────────────────────
    // BEFORE_COMMIT: Outbox 저장 후 eventId를 트랜잭션 컨텍스트에 바인딩
    // ──────────────────────────────────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatOccupied(event: SeatOccupiedEvent) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) return
        val outbox = sseOutboxService.saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.HOLD.name)
        if (outbox != null) {
            bindEventId(event.scheduleId, event.seatNumber, outbox.eventId)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatReleased(event: SeatReleasedEvent) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) return
        val outbox = sseOutboxService.saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
        if (outbox != null) {
            bindEventId(event.scheduleId, event.seatNumber, outbox.eventId)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatExpired(event: SeatExpiredEvent) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) return
        val outbox = sseOutboxService.saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name)
        if (outbox != null) {
            bindEventId(event.scheduleId, event.seatNumber, outbox.eventId)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    fun saveOutboxOnSeatSold(event: SeatSoldEvent) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) return
        val outbox = sseOutboxService.saveOutboxEvent(event.scheduleId, event.seatNumber, SeatStatus.SOLD_OUT.name)
        if (outbox != null) {
            bindEventId(event.scheduleId, event.seatNumber, outbox.eventId)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AFTER_COMMIT: eventId 조회 후 SSE 브로드캐스트 및 ETag 버전 증가
    // ──────────────────────────────────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatOccupied(event: SeatOccupiedEvent) {
        val eventId = unbindEventId(event.scheduleId, event.seatNumber)
        log.info("SSE 브로드캐스트 (선점): scheduleId={}, seat={}, eventId={}", event.scheduleId, event.seatNumber, eventId)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.HOLD.name, eventId)
        eTagVersionManager.increment(event.scheduleId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatReleased(event: SeatReleasedEvent) {
        val eventId = unbindEventId(event.scheduleId, event.seatNumber)
        log.info("SSE 브로드캐스트 (선점 취소): scheduleId={}, seat={}, eventId={}", event.scheduleId, event.seatNumber, eventId)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name, eventId)
        eTagVersionManager.increment(event.scheduleId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatExpired(event: SeatExpiredEvent) {
        val eventId = unbindEventId(event.scheduleId, event.seatNumber)
        log.info("SSE 브로드캐스트 (만료 복구): scheduleId={}, seat={}, eventId={}", event.scheduleId, event.seatNumber, eventId)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.AVAILABLE.name, eventId)
        eTagVersionManager.increment(event.scheduleId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatSold(event: SeatSoldEvent) {
        val eventId = unbindEventId(event.scheduleId, event.seatNumber)
        log.info("SSE 브로드캐스트 (결제 판매완료): scheduleId={}, seat={}, eventId={}", event.scheduleId, event.seatNumber, eventId)
        registry.broadcast(event.scheduleId, event.seatNumber, SeatStatus.SOLD_OUT.name, eventId)
        eTagVersionManager.increment(event.scheduleId)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AFTER_ROLLBACK: 트랜잭션 롤백 시 바인딩 해제 (메모리 누수 방지)
    // ──────────────────────────────────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun cleanupThreadLocalOnOccupiedRollback(event: SeatOccupiedEvent) {
        unbindEventId(event.scheduleId, event.seatNumber)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun cleanupThreadLocalOnReleasedRollback(event: SeatReleasedEvent) {
        unbindEventId(event.scheduleId, event.seatNumber)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun cleanupThreadLocalOnExpiredRollback(event: SeatExpiredEvent) {
        unbindEventId(event.scheduleId, event.seatNumber)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun cleanupThreadLocalOnSoldRollback(event: SeatSoldEvent) {
        unbindEventId(event.scheduleId, event.seatNumber)
    }

    @EventListener
    fun onPaymentCompleted(event: PaymentCompletedEvent) {
        log.debug("SSE 브로드캐스트 (결제 완료): scheduleId={}", event.scheduleId)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: TransactionSynchronizationManager 바인딩/해제 (타입 안전한 Object Key & Holder 사용)
    // ──────────────────────────────────────────────────────────────────────────

    private fun seatKey(scheduleId: Long, seatNumber: String) = "$scheduleId:$seatNumber"

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreateHolder(): ConcurrentHashMap<String, String> {
        var holder = TransactionSynchronizationManager.getResource(SseBroadcasterTxKey) as? ConcurrentHashMap<String, String>
        if (holder == null) {
            holder = ConcurrentHashMap()
            TransactionSynchronizationManager.bindResource(SseBroadcasterTxKey, holder)
        }
        return holder
    }

    private fun bindEventId(scheduleId: Long, seatNumber: String, eventId: String) {
        getOrCreateHolder()[seatKey(scheduleId, seatNumber)] = eventId
    }

    @Suppress("UNCHECKED_CAST")
    private fun unbindEventId(scheduleId: Long, seatNumber: String): String? {
        val holder = TransactionSynchronizationManager.getResource(SseBroadcasterTxKey) as? ConcurrentHashMap<String, String>
        val eventId = holder?.remove(seatKey(scheduleId, seatNumber))
        if (holder != null && holder.isEmpty()) {
            TransactionSynchronizationManager.unbindResourceIfPossible(SseBroadcasterTxKey)
        }
        return eventId
    }

    companion object {
        private object SseBroadcasterTxKey
    }
}
