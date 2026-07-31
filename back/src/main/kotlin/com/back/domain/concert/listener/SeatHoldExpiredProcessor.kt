package com.back.domain.concert.listener

import com.back.domain.concert.event.SeatExpiredEvent
import com.back.domain.concert.service.SeatOccupyManager
import com.back.domain.schedule.constant.SeatStatus
import com.back.domain.schedule.repository.ScheduleSeatRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Service
class SeatHoldExpiredProcessor(
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val seatOccupyManager: SeatOccupyManager,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

     // 1초 스케줄러에서 호출
     // SKIP LOCKED를 사용하여 다른 트랜잭션(결제/선점)이 해당 좌석을 처리 중이면 즉시 Skip
     // Skip된 좌석은 다음 스케줄러 주기(1초 후)에 다시 시도
    @Transactional
    fun processExpiredSeat(concertId: Long, scheduleId: Long, seatNumber: String) {
        log.debug("좌석 선점 만료 처리 시도: concertId={}, scheduleId={}, seat={}", concertId, scheduleId, seatNumber)

        // SKIP LOCKED: 다른 트랜잭션이 이 좌석을 처리 중이면 null 반환 → 이번 주기 Skip
        val seat = scheduleSeatRepository.findWithSkipLockByScheduleIdAndSeatNumber(scheduleId, seatNumber)

        if (seat == null) {
            log.debug("좌석 SKIP (다른 트랜잭션 처리 중): scheduleId={}, seat={}", scheduleId, seatNumber)
            return
        }

        if (seat.seatStatus != SeatStatus.HOLD) {
            // 이미 SOLD_OUT 또는 AVAILABLE 상태 → 만료 큐에서만 제거
            log.debug("좌석이 HOLD 상태가 아님 (이미 처리됨): scheduleId={}, seat={}, status={}", scheduleId, seatNumber, seat.seatStatus)
            seatOccupyManager.cleanupRedis(SeatOccupyManager.generateSeatOccupyKey(concertId, scheduleId, seatNumber))
            return
        }

        seat.releaseToAvailable()
        log.info("좌석 복구 완료 (HOLD → AVAILABLE): scheduleId={}, seat={}", scheduleId, seatNumber)
        eventPublisher.publishEvent(SeatExpiredEvent(concertId, scheduleId, seatNumber))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSeatExpiredCleanupRedis(event: SeatExpiredEvent) {
        val redisKey = SeatOccupyManager.generateSeatOccupyKey(event.concertId, event.scheduleId, event.seatNumber)

        try {
            seatOccupyManager.cleanupRedis(redisKey)
            seatOccupyManager.removeFromExpireQueue(event.concertId, event.scheduleId, event.seatNumber)
            log.debug("만료 좌석 Redis 정리 완료: {}", redisKey)
        } catch (e: Exception) {
            log.warn("만료 좌석 Redis 정리 실패 (무시됨): {}", redisKey, e)
        }
    }
}
