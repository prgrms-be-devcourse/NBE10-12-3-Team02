package com.back.domain.waiting.service

import com.back.domain.concert.service.ConcertService
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.domain.user.repository.UserRepository
import com.back.domain.waiting.dto.QueueConnectionState
import com.back.domain.waiting.outbox.service.QueueSseOutboxPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.time.Duration

class WaitingQueueConnectionStateTest {
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
    @DisplayName("이미 입장한 사용자는 SSE 연결 시 기존 입장 토큰을 복구한다")
    fun t1() {
        `when`(waitingQueueManager.getConnectionSnapshot(SCHEDULE_ID, USER_ID))
            .thenReturn(QueueConnectionSnapshot.Active("entry-token"))

        val result = service.getConnectionStateAfterValidation(CONCERT_ID, SCHEDULE_ID, USER_ID)

        assertThat(result.state).isEqualTo(QueueConnectionState.ACTIVE)
        assertThat(result.entryToken).isEqualTo("entry-token")
        verify(waitingQueueManager, never()).findWaitingRank(SCHEDULE_ID, USER_ID)
        verifyNoInteractions(userRepository, concertService)
    }

    @Test
    @DisplayName("대기 중인 사용자는 SSE 연결 시 현재 순번을 복구한다")
    fun t2() {
        `when`(waitingQueueManager.getConnectionSnapshot(SCHEDULE_ID, USER_ID))
            .thenReturn(QueueConnectionSnapshot.Waiting(3L, 7L))

        val result = service.getConnectionStateAfterValidation(CONCERT_ID, SCHEDULE_ID, USER_ID)

        assertThat(result.state).isEqualTo(QueueConnectionState.WAITING)
        assertThat(result.rank).isEqualTo(3L)
        assertThat(result.myQueueNumber).isEqualTo(7L)
        assertThat(result.entryToken).isNull()
    }

    @Test
    @DisplayName("대기열에 없는 사용자는 SSE 연결 시 미등록 상태를 받는다")
    fun t3() {
        `when`(waitingQueueManager.getConnectionSnapshot(SCHEDULE_ID, USER_ID))
            .thenReturn(QueueConnectionSnapshot.NotRegistered)

        val result = service.getConnectionStateAfterValidation(CONCERT_ID, SCHEDULE_ID, USER_ID)

        assertThat(result.state).isEqualTo(QueueConnectionState.NOT_REGISTERED)
        assertThat(result.rank).isZero()
        assertThat(result.entryToken).isNull()
    }

    companion object {
        private const val CONCERT_ID = 1L
        private const val SCHEDULE_ID = 10L
        private const val USER_ID = 101L
    }
}
