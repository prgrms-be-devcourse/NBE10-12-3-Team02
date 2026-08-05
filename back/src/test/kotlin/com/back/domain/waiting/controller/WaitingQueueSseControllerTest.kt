package com.back.domain.waiting.controller

import com.back.domain.waiting.event.EntryAllowedEvent
import com.back.domain.waiting.dto.QueueConnectionEvent
import com.back.domain.waiting.constant.QueueConnectionState
import com.back.domain.waiting.service.WaitingQueueService
import com.back.domain.waiting.sse.QueueSseEmitterRegistry
import com.back.global.RedisTestConfig
import com.back.global.security.SecurityUser
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(RedisTestConfig::class)
class WaitingQueueSseControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val registry: QueueSseEmitterRegistry,
) {
    @MockitoBean
    private lateinit var waitingQueueService: WaitingQueueService

    @Test
    @DisplayName("인증하지 않은 대기열 SSE 구독 요청은 401을 반환한다")
    fun t1() {
        mockMvc.perform(
            get(SSE_URL, CONCERT_ID, SCHEDULE_ID)
                .accept(MediaType.TEXT_EVENT_STREAM),
        )
            .andExpect(status().isUnauthorized)

        verifyNoInteractions(waitingQueueService)
    }

    @Test
    @DisplayName("인증된 SSE 구독은 이벤트 스트림 헤더와 connected 이벤트를 반환한다")
    fun t2() {
        val connectionState = QueueConnectionEvent(
            concertId = CONCERT_ID,
            scheduleId = SCHEDULE_ID,
            userId = USER_ID,
            state = QueueConnectionState.WAITING,
            rank = 3L,
            myQueueNumber = 7L,
            entryToken = null,
        )
        `when`(
            waitingQueueService.getConnectionStateAfterValidation(CONCERT_ID, SCHEDULE_ID, USER_ID),
        ).thenReturn(connectionState)

        val mvcResult = mockMvc.perform(
            get(SSE_URL, CONCERT_ID, SCHEDULE_ID)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .with(user(SecurityUser(USER_ID, "대기 사용자"))),
        )
            .andExpect(request().asyncStarted())
            .andReturn()

        registry.sendEntryAllowed(
            EntryAllowedEvent(SCHEDULE_ID, USER_ID, "entry-token", System.currentTimeMillis() + 60_000L),
        )

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
            .andExpect(header().string("X-Accel-Buffering", "no"))
            .andExpect(content().string(containsString("event:connected")))
            .andExpect(content().string(containsString("event:entry-allowed")))

        verify(waitingQueueService).validateSseSubscription(CONCERT_ID, SCHEDULE_ID, USER_ID)
        verify(waitingQueueService).getConnectionStateAfterValidation(CONCERT_ID, SCHEDULE_ID, USER_ID)
    }

    companion object {
        private const val SSE_URL = "/api/v1/waiting/concerts/{concertId}/schedules/{scheduleId}/events"
        private const val CONCERT_ID = 1L
        private const val SCHEDULE_ID = 10L
        private const val USER_ID = 101L
    }
}
