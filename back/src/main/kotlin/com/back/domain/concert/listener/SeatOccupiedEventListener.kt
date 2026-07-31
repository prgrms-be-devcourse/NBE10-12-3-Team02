package com.back.domain.concert.listener

import com.back.domain.concert.event.SeatOccupiedEvent
import com.back.domain.concert.service.SeatOccupyManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SeatOccupiedEventListener(
    private val seatOccupyManager: SeatOccupyManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSeatOccupied(event: SeatOccupiedEvent) {
        try {
            seatOccupyManager.addToExpireQueue(event.concertId, event.scheduleId, event.seatNumber, event.ttlSeconds)
        } catch (e: Exception) {
            log.warn("만료 큐 등록 실패 (무시됨): concertId={}, scheduleId={}, seat={}", event.concertId, event.scheduleId, event.seatNumber, e)
        }
    }
}
