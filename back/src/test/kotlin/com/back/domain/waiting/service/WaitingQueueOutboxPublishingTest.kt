package com.back.domain.waiting.service

import com.back.domain.concert.service.ConcertService
import com.back.domain.queue.event.EntryAllowedEvent
import com.back.domain.queue.event.QueueErrorEvent
import com.back.domain.schedule.constant.SeatStatus
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.domain.user.repository.UserRepository
import com.back.domain.waiting.dto.QueueStatusDto
import com.back.domain.waiting.outbox.service.QueueSseOutboxPublisher
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.time.Duration

class WaitingQueueOutboxPublishingTest {
    private val waitingQueueManager = mock(WaitingQueueManager::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val concertService = mock(ConcertService::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val outboxPublisher = mock(QueueSseOutboxPublisher::class.java)
    private val scheduleSeatRepository = mock(ScheduleSeatRepository::class.java)
    private val service = WaitingQueueService(
        waitingQueueManager,
        userRepository,
        concertService,
        eventPublisher,
        outboxPublisher,
        scheduleSeatRepository,
        Duration.ofMinutes(10),
        2,
        5,
    )

    @Test
    @DisplayName("입장 허용 이벤트는 Spring 메모리 이벤트 대신 Outbox에 저장한다")
    fun t1() {
        val admission = QueueAdmission(USER_ID, "entry-token", System.currentTimeMillis() + 600_000L)
        `when`(
            scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatus(SCHEDULE_ID, SeatStatus.AVAILABLE),
        ).thenReturn(1L)
        `when`(waitingQueueManager.addActiveUser(SCHEDULE_ID, 1L, 2, Duration.ofMinutes(10)))
            .thenReturn(listOf(admission))
        `when`(waitingQueueManager.getQueueStatus(SCHEDULE_ID)).thenReturn(QueueStatusDto(0L, 0L))

        service.allowEntry(CONCERT_ID, SCHEDULE_ID)

        verify(outboxPublisher).publishEntryAllowed(
            EntryAllowedEvent(SCHEDULE_ID, USER_ID, admission.entryToken, admission.expiredAt),
        )
    }

    @Test
    @DisplayName("회차가 매진되면 종료 오류를 Outbox에 저장하고 대기열을 정리한다")
    fun t2() {
        `when`(
            scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatus(SCHEDULE_ID, SeatStatus.AVAILABLE),
        ).thenReturn(0L)
        `when`(
            scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatus(SCHEDULE_ID, SeatStatus.HOLD),
        ).thenReturn(0L)
        `when`(waitingQueueManager.getQueueStatus(SCHEDULE_ID)).thenReturn(QueueStatusDto(0L, 3L))
        val expected = QueueErrorEvent(SCHEDULE_ID, null, "콘서트가 매진되어 대기열이 종료되었습니다.")

        service.allowEntry(CONCERT_ID, SCHEDULE_ID)

        verify(outboxPublisher).publishQueueError(expected)
        verify(waitingQueueManager).clearWaitingQueue(SCHEDULE_ID)
    }

    @Test
    @DisplayName("AVAILABLE 좌석이 없어도 HOLD 좌석이 남아 있으면 대기열을 유지한다")
    fun t3() {
        `when`(
            scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatus(SCHEDULE_ID, SeatStatus.AVAILABLE),
        ).thenReturn(0L)
        `when`(
            scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatus(SCHEDULE_ID, SeatStatus.HOLD),
        ).thenReturn(2L)

        service.allowEntry(CONCERT_ID, SCHEDULE_ID)

        verify(waitingQueueManager, never()).clearWaitingQueue(SCHEDULE_ID)
        verifyNoInteractions(outboxPublisher, eventPublisher)
    }

    @Test
    @DisplayName("Outbox 저장 장애 시 입장 알림을 기존 Spring 이벤트로 대체 발행한다")
    fun t4() {
        val admission = QueueAdmission(USER_ID, "entry-token", System.currentTimeMillis() + 600_000L)
        val expected = EntryAllowedEvent(SCHEDULE_ID, USER_ID, admission.entryToken, admission.expiredAt)
        `when`(
            scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatus(SCHEDULE_ID, SeatStatus.AVAILABLE),
        ).thenReturn(1L)
        `when`(waitingQueueManager.addActiveUser(SCHEDULE_ID, 1L, 2, Duration.ofMinutes(10)))
            .thenReturn(listOf(admission))
        `when`(waitingQueueManager.getQueueStatus(SCHEDULE_ID)).thenReturn(QueueStatusDto(0L, 0L))
        doThrow(IllegalStateException("database unavailable"))
            .`when`(outboxPublisher)
            .publishEntryAllowed(expected)

        service.allowEntry(CONCERT_ID, SCHEDULE_ID)

        verify(eventPublisher).publishEvent(expected)
    }

    companion object {
        private const val CONCERT_ID = 1L
        private const val SCHEDULE_ID = 10L
        private const val USER_ID = 101L
    }
}
