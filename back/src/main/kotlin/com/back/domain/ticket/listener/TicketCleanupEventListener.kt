package com.back.domain.ticket.listener

import com.back.domain.concert.event.SeatReleasedEvent
import com.back.domain.concert.service.SeatOccupyManager
import com.back.domain.ticket.event.SeatPurchasedCleanupEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class TicketCleanupEventListener(
    private val seatOccupyManager: SeatOccupyManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatPurchasedCleanup(event: SeatPurchasedCleanupEvent) {
        log.debug("DB 커밋 완료 후 Redis 선점 해제 진행 (결제): scheduleId={}, count={}", event.scheduleId, event.seatHolds.size)
        for (holdInfo in event.seatHolds) {
            val redisKey = SeatOccupyManager.generateSeatOccupyKey(event.concertId, event.scheduleId, holdInfo.seatNumber)
            seatOccupyManager.cleanupRedis(redisKey)
            seatOccupyManager.removeFromExpireQueue(event.concertId, event.scheduleId, holdInfo.seatNumber)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onSeatReleasedCleanup(event: SeatReleasedEvent) {
        log.debug("DB 커밋 완료 후 Redis 선점 해제 진행 (취소/해제): scheduleId={}, seat={}", event.scheduleId, event.seatNumber)
        val redisKey = SeatOccupyManager.generateSeatOccupyKey(event.concertId, event.scheduleId, event.seatNumber)
        seatOccupyManager.cleanupRedis(redisKey)
        seatOccupyManager.removeFromExpireQueue(event.concertId, event.scheduleId, event.seatNumber)
    }
}
