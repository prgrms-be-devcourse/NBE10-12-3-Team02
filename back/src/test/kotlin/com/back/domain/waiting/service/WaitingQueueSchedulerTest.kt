package com.back.domain.waiting.service

import com.back.domain.concert.entity.Concert
import com.back.domain.schedule.entity.Schedule
import com.back.domain.schedule.repository.ScheduleRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class WaitingQueueSchedulerTest {
    private val waitingQueueService = mock(WaitingQueueService::class.java)
    private val waitingQueueManager = mock(WaitingQueueManager::class.java)
    private val scheduleRepository = mock(ScheduleRepository::class.java)
    private val scheduler = WaitingQueueScheduler(
        waitingQueueService,
        waitingQueueManager,
        scheduleRepository,
    )

    @Test
    @DisplayName("이번 주기에 만료된 사용자가 없어도 활성 회차의 입장 인원을 재조정한다")
    fun t1() {
        val schedule = schedule(CONCERT_ID)
        `when`(waitingQueueManager.getActiveScheduleIds()).thenReturn(setOf(SCHEDULE_ID.toString()))
        `when`(waitingQueueManager.hasWaitingUsers(SCHEDULE_ID)).thenReturn(true)
        `when`(scheduleRepository.findByScheduleId(SCHEDULE_ID)).thenReturn(schedule)
        `when`(waitingQueueManager.removeExpiredActiveUsers(SCHEDULE_ID)).thenReturn(0L)

        scheduler.processExpiredActiveUsers()

        verify(waitingQueueService).allowEntry(CONCERT_ID, SCHEDULE_ID)
    }

    @Test
    @DisplayName("한 회차 처리에 실패해도 다음 활성 회차는 계속 처리한다")
    fun t2() {
        val secondScheduleId = 20L
        val secondConcertId = 2L
        val secondSchedule = schedule(secondConcertId)
        `when`(waitingQueueManager.getActiveScheduleIds())
            .thenReturn(linkedSetOf(SCHEDULE_ID.toString(), secondScheduleId.toString()))
        `when`(waitingQueueManager.hasWaitingUsers(SCHEDULE_ID)).thenReturn(true)
        `when`(waitingQueueManager.hasWaitingUsers(secondScheduleId)).thenReturn(true)
        `when`(scheduleRepository.findByScheduleId(SCHEDULE_ID))
            .thenThrow(IllegalStateException("database unavailable"))
        `when`(scheduleRepository.findByScheduleId(secondScheduleId))
            .thenReturn(secondSchedule)

        scheduler.processExpiredActiveUsers()

        verify(waitingQueueService).allowEntry(secondConcertId, secondScheduleId)
    }

    @Test
    @DisplayName("회차 정보를 조회한 후 만료 ACTIVE 사용자 제거와 입장 재조정을 수행한다")
    fun t3() {
        val schedule = schedule(CONCERT_ID)
        `when`(waitingQueueManager.getActiveScheduleIds()).thenReturn(setOf(SCHEDULE_ID.toString()))
        `when`(waitingQueueManager.hasWaitingUsers(SCHEDULE_ID)).thenReturn(true)
        `when`(scheduleRepository.findByScheduleId(SCHEDULE_ID)).thenReturn(schedule)

        scheduler.processExpiredActiveUsers()

        inOrder(scheduleRepository, waitingQueueManager, waitingQueueService).run {
            verify(scheduleRepository).findByScheduleId(SCHEDULE_ID)
            verify(waitingQueueManager).removeExpiredActiveUsers(SCHEDULE_ID)
            verify(waitingQueueService).allowEntry(CONCERT_ID, SCHEDULE_ID)
        }
    }

    @Test
    @DisplayName("대기자가 없으면 DB 조회와 입장 재조정을 생략하고 Redis 만료 상태만 정리한다")
    fun t4() {
        `when`(waitingQueueManager.getActiveScheduleIds()).thenReturn(setOf(SCHEDULE_ID.toString()))
        `when`(waitingQueueManager.hasWaitingUsers(SCHEDULE_ID)).thenReturn(false)

        scheduler.processExpiredActiveUsers()

        verify(waitingQueueManager).removeExpiredActiveUsers(SCHEDULE_ID)
        verify(waitingQueueManager).isQueueEmpty(SCHEDULE_ID)
        verifyNoInteractions(scheduleRepository, waitingQueueService)
    }

    private fun schedule(concertId: Long): Schedule {
        val concert = mock(Concert::class.java)
        val schedule = mock(Schedule::class.java)
        `when`(concert.concertId).thenReturn(concertId)
        `when`(schedule.concert).thenReturn(concert)
        return schedule
    }

    companion object {
        private const val CONCERT_ID = 1L
        private const val SCHEDULE_ID = 10L
    }
}
