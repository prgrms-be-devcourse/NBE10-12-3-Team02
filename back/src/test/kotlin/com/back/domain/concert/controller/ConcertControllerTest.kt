package com.back.domain.concert.controller

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.schedule.constant.SeatStatus.AVAILABLE
import com.back.domain.schedule.constant.SeatStatus.HOLD
import com.back.domain.schedule.entity.Schedule
import com.back.domain.schedule.entity.ScheduleSeat
import com.back.domain.schedule.repository.ScheduleRepository
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.domain.venue.entity.Venue
import com.back.domain.venue.repository.VenueRepository
import com.back.global.security.SecurityUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConcertControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val concertRepository: ConcertRepository,
    private val venueRepository: VenueRepository,
    private val scheduleRepository: ScheduleRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository
) {
    private lateinit var concert: Concert
    private lateinit var schedule: Schedule

    @MockitoBean
    private lateinit var stringRedisTemplate: StringRedisTemplate

    private lateinit var zSetOps: ZSetOperations<String, String>
    private lateinit var valOps: ValueOperations<String, String>
    private lateinit var hashOps: HashOperations<String, Any, Any>

    @BeforeEach
    @Suppress("UNCHECKED_CAST")
    fun setUp() {
        zSetOps = mock(ZSetOperations::class.java) as ZSetOperations<String, String>
        valOps = mock(ValueOperations::class.java) as ValueOperations<String, String>
        hashOps = mock(HashOperations::class.java) as HashOperations<String, Any, Any>

        doReturn(zSetOps).`when`(stringRedisTemplate).opsForZSet()
        doReturn(valOps).`when`(stringRedisTemplate).opsForValue()
        doReturn(hashOps).`when`(stringRedisTemplate).opsForHash<Any, Any>()

        `when`(zSetOps.score(anyString(), anyString())).thenReturn((System.currentTimeMillis() + 600000).toDouble())
        `when`(valOps.get(anyString())).thenReturn("test-queue-token")

        concert = Concert.create("아이유 콘서트", "설명", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "poster.jpg")
        concertRepository.save(concert)

        val venue = Venue.create("올림픽체조경기장", "서울", 15000L)
        venueRepository.save(venue)

        schedule = Schedule.create(concert, venue, LocalDateTime.now().plusDays(7), 1)
        scheduleRepository.save(schedule)
    }

    @Test
    @DisplayName("좌석 선택 페이지 조회 성공")
    fun t1() {
        val seat1 = ScheduleSeat.create(schedule, "VIP", "A-1", 150000, AVAILABLE)
        scheduleSeatRepository.save(seat1)

        val seat2 = ScheduleSeat.create(schedule, "A", "B-2", 70000, AVAILABLE)
        scheduleSeatRepository.save(seat2)

        mockMvc.perform(
            get("/api/v1/concerts/{concertId}/schedules/{scheduleId}/seats", concert.concertId, schedule.scheduleId)
                .header("X-Queue-Token", "test-queue-token")
                .with(user(SecurityUser(1L, "테스트유저")))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.concertId").value(concert.concertId))
            .andExpect(jsonPath("$.data.scheduleId").value(schedule.scheduleId))
            .andExpect(jsonPath("$.data.seats").isArray)
            .andExpect(jsonPath("$.data.seats.length()").value(2))
    }

    @Test
    @DisplayName("존재하지 않는 공연 일정 좌석 선택 시 예외 발생")
    fun t2() {
        mockMvc.perform(
            get("/api/v1/concerts/{concertId}/schedules/{scheduleId}/seats", concert.concertId, 999L)
                .header("X-Queue-Token", "test-queue-token")
                .with(user(SecurityUser(1L, "테스트유저")))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("공연과 일정 매칭 실패 시 예외 발생")
    fun t3() {
        val otherConcert = Concert.create("다른 콘서트", "설명", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "poster2.jpg")
        concertRepository.save(otherConcert)

        mockMvc.perform(
            get("/api/v1/concerts/{concertId}/schedules/{scheduleId}/seats", otherConcert.concertId, schedule.scheduleId)
                .header("X-Queue-Token", "test-queue-token")
                .with(user(SecurityUser(1L, "테스트유저")))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("좌석 임시 선점 성공")
    @Suppress("UNCHECKED_CAST")
    fun t4() {
        val seat = ScheduleSeat.create(schedule, "VIP", "A-1", 150000, AVAILABLE)
        scheduleSeatRepository.save(seat)

        `when`(stringRedisTemplate.execute(any<RedisScript<Long>>(), anyList(), any(), any(), any())).thenReturn(1L)

        val requestBody = """
            {
              "seatNumber": "A-1"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/concerts/{concertId}/schedules/{scheduleId}/seats/occupy", concert.concertId, schedule.scheduleId)
                .header("X-Queue-Token", "test-queue-token")
                .with(user(SecurityUser(1L, "테스트유저")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("좌석 임시 선점에 성공했습니다."))
            .andExpect(jsonPath("$.data.occupyToken").isString)
            .andExpect(jsonPath("$.data.expireInSeconds").value(600))
            .andExpect(jsonPath("$.data.seatStatus").value("HOLD"))
    }

    @Test
    @DisplayName("좌석 임시 선점 취소 성공")
    @Suppress("UNCHECKED_CAST")
    fun t5() {
        val userId = 1L
        val seatNumber = "A-1"

        val seat = ScheduleSeat.create(schedule, "VIP", seatNumber, 150000, HOLD)
        scheduleSeatRepository.save(seat)

        val entries: Map<Any, Any> = mapOf("userId" to userId.toString())
        doReturn(entries).`when`(hashOps).entries(anyString())

        val requestBody = """
            {
              "seatNumber": "A-1"
            }
        """.trimIndent()

        mockMvc.perform(
            delete("/api/v1/concerts/{concertId}/schedules/{scheduleId}/seats/occupy", concert.concertId, schedule.scheduleId)
                .with(user(SecurityUser(userId, "테스트유저")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("좌석 선점이 정상적으로 취소되었습니다."))
    }
}
