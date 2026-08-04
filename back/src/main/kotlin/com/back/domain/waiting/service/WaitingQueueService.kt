package com.back.domain.waiting.service

import com.back.domain.concert.service.ConcertService
import com.back.domain.waiting.event.EntryAllowedEvent
import com.back.domain.waiting.event.QueueErrorEvent
import com.back.domain.waiting.event.QueueStatusEvent
import com.back.domain.schedule.constant.SeatStatus
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.domain.user.repository.UserRepository
import com.back.domain.waiting.dto.WaitingQueueResponse
import com.back.domain.waiting.dto.QueueConnectionEvent
import com.back.domain.waiting.dto.QueueConnectionState
import com.back.domain.waiting.outbox.service.QueueSseOutboxPublisher
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class WaitingQueueService(
    private val waitingQueueManager: WaitingQueueManager,
    private val userRepository: UserRepository,
    private val concertService: ConcertService,
    private val eventPublisher: ApplicationEventPublisher,
    private val outboxPublisher: QueueSseOutboxPublisher,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    @Value("\${queue.entry-token.ttl}") private val entryTokenTtl: Duration,
    @Value("\${queue.batch-size}") private val batchSize: Int,
    @Value("\${queue.max-active-users}") private val maxActiveUsers: Int
) {

    fun registerWaiting(concertId: Long, scheduleId: Long, userId: Long): WaitingQueueResponse {
        validateUser(userId)
        concertService.validateConcertScheduleMatch(concertId, scheduleId)
        concertService.validateScheduleBookable(scheduleId)

        val remainingSeats = scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatusIn(
            scheduleId,
            listOf(SeatStatus.AVAILABLE, SeatStatus.HOLD)
        )
        if (remainingSeats <= 0) {
            throw ServiceException(ErrorCode.CONCERT_SOLD_OUT)
        }

        waitingQueueManager.getActiveToken(scheduleId, userId)?.let { activeToken ->
            return WaitingQueueResponse.of(concertId, scheduleId, userId, 0L, 0L, activeToken)
        }

        val rank = waitingQueueManager.registerWaiting(scheduleId, userId)
        val myQueueNumber = waitingQueueManager.getQueueSequence(scheduleId, userId)

        allowEntry(concertId, scheduleId)

        return WaitingQueueResponse.of(
            concertId,
            scheduleId,
            userId,
            rank,
            myQueueNumber,
            null
        )
    }

    fun showWaitingRank(concertId: Long, scheduleId: Long, userId: Long): WaitingQueueResponse {
        validateUser(userId)
        concertService.validateConcertScheduleMatch(concertId, scheduleId)

        waitingQueueManager.getActiveToken(scheduleId, userId)?.let { activeToken ->
            return WaitingQueueResponse.of(concertId, scheduleId, userId, 0L, 0L, activeToken)
        }

        val rank = waitingQueueManager.showWaitingRank(scheduleId, userId)
        val myQueueNumber = waitingQueueManager.getQueueSequence(scheduleId, userId)

        return WaitingQueueResponse.of(
            concertId,
            scheduleId,
            userId,
            rank,
            myQueueNumber,
            null
        )
    }

    fun getConnectionStateAfterValidation(concertId: Long, scheduleId: Long, userId: Long): QueueConnectionEvent {
        return when (val snapshot = waitingQueueManager.getConnectionSnapshot(scheduleId, userId)) {
            is QueueConnectionSnapshot.Active -> QueueConnectionEvent(
                concertId, scheduleId, userId, QueueConnectionState.ACTIVE,
                0L, 0L, snapshot.entryToken,
            )

            is QueueConnectionSnapshot.Waiting -> QueueConnectionEvent(
                concertId, scheduleId, userId, QueueConnectionState.WAITING,
                snapshot.rank, snapshot.myQueueNumber, null,
            )

            QueueConnectionSnapshot.NotRegistered -> QueueConnectionEvent(
                concertId, scheduleId, userId, QueueConnectionState.NOT_REGISTERED,
                0L, 0L, null,
            )
        }
    }

    fun validateSseSubscription(concertId: Long, scheduleId: Long, userId: Long) {
        validateUser(userId)
        concertService.validateConcertScheduleMatch(concertId, scheduleId)
    }

    fun cancelWaiting(concertId: Long, scheduleId: Long, userId: Long) {
        validateUser(userId)
        concertService.validateConcertScheduleMatch(concertId, scheduleId)

        val removedFromWaiting = waitingQueueManager.cancelWaiting(scheduleId, userId)
        val removedFromActive = !removedFromWaiting && waitingQueueManager.cancelActiveUser(scheduleId, userId)

        if (!removedFromWaiting && !removedFromActive) {
            throw ServiceException(ErrorCode.WAITING_QUEUE_NOT_FOUND)
        }

        if (removedFromActive) {
            allowEntry(concertId, scheduleId)
        } else {
            publishQueueRank(scheduleId)
        }
    }

    fun allowEntry(concertId: Long, scheduleId: Long) {
        concertService.validateConcertScheduleMatch(concertId, scheduleId)

        val remainingSeats = scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatus(
            scheduleId, SeatStatus.AVAILABLE
        )

        if (remainingSeats <= 0) {
            val heldSeats = scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatus(
                scheduleId,
                SeatStatus.HOLD,
            )

            // HOLD 좌석은 결제 미완료 상태이며 만료 또는 취소 후 다시 판매될 수 있다.
            // 추가 입장만 잠시 멈추고, 실제 결제 완료 좌석만 남을 때까지 대기열은 유지한다.
            if (heldSeats > 0) return

            val status = waitingQueueManager.getQueueStatus(scheduleId)
            if (status.totalWaitingCount > 0) {
                publishQueueError(
                    QueueErrorEvent(scheduleId, null, "콘서트가 매진되어 대기열이 종료되었습니다."),
                )
                waitingQueueManager.clearWaitingQueue(scheduleId)
            }
            return
        }

        val capacity = minOf(remainingSeats, maxActiveUsers.toLong())
        val admissions = waitingQueueManager.addActiveUser(scheduleId, capacity, batchSize, entryTokenTtl)

        for (admission in admissions) {
            publishEntryAllowed(
                EntryAllowedEvent(scheduleId, admission.userId, admission.entryToken, admission.expiredAt),
            )
        }
        publishQueueRank(scheduleId)
    }

    private fun publishQueueRank(scheduleId: Long) {
        val status = waitingQueueManager.getQueueStatus(scheduleId)

        if (status.totalWaitingCount == 0L) return

        eventPublisher.publishEvent(
            QueueStatusEvent.of(
                scheduleId,
                status.currentAllowedSequence,
                status.totalWaitingCount
            )
        )
    }

    private fun validateUser(userId: Long) {
        userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)
    }

    private fun publishEntryAllowed(event: EntryAllowedEvent) {
        try {
            outboxPublisher.publishEntryAllowed(event)
        } catch (e: Exception) {
            log.error(
                "입장 허용 Outbox 저장 실패로 인메모리 이벤트 대체 발행: scheduleId={}, userId={}",
                event.scheduleId,
                event.userId,
                e,
            )
            eventPublisher.publishEvent(event)
        }
    }

    private fun publishQueueError(event: QueueErrorEvent) {
        try {
            outboxPublisher.publishQueueError(event)
        } catch (e: Exception) {
            log.error(
                "대기열 오류 Outbox 저장 실패로 인메모리 이벤트 대체 발행: scheduleId={}, userId={}",
                event.scheduleId,
                event.userId,
                e,
            )
            eventPublisher.publishEvent(event)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(WaitingQueueService::class.java)
    }
}
