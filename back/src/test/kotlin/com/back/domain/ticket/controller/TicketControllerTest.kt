package com.back.domain.ticket.controller

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.schedule.constant.SeatStatus.HOLD
import com.back.domain.schedule.entity.Schedule
import com.back.domain.schedule.entity.ScheduleSeat
import com.back.domain.schedule.repository.ScheduleRepository
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.domain.ticket.entity.Ticket
import com.back.domain.ticket.repository.TicketRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.domain.venue.entity.Venue
import com.back.domain.venue.repository.VenueRepository
import com.back.global.security.SecurityUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
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
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
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
class TicketControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val concertRepository: ConcertRepository,
    private val venueRepository: VenueRepository,
    private val scheduleRepository: ScheduleRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val ticketRepository: TicketRepository,
) {
    private lateinit var concert: Concert
    private lateinit var schedule: Schedule
    private lateinit var seat: ScheduleSeat
    private lateinit var userEntity: User
    private lateinit var securityUser: SecurityUser

    @MockitoBean
    private lateinit var stringRedisTemplate: StringRedisTemplate

    private lateinit var zSetOps: ZSetOperations<String, String>
    private lateinit var valOps: ValueOperations<String, String>
    private lateinit var hashOps: HashOperations<String, String, String>

    @BeforeEach
    @Suppress("UNCHECKED_CAST")
    fun setUp() {
        userEntity = saveUser()
        securityUser = SecurityUser(userEntity.userId!!, userEntity.name)

        zSetOps = mock(ZSetOperations::class.java) as ZSetOperations<String, String>
        valOps = mock(ValueOperations::class.java) as ValueOperations<String, String>
        hashOps = mock(HashOperations::class.java) as HashOperations<String, String, String>

        doReturn(zSetOps).`when`(stringRedisTemplate).opsForZSet()
        doReturn(valOps).`when`(stringRedisTemplate).opsForValue()
        doReturn(hashOps).`when`(stringRedisTemplate).opsForHash<String, String>()

        doReturn((System.currentTimeMillis() + 600000).toDouble()).`when`(zSetOps).score(anyString(), anyString())
        doReturn("test-queue-token").`when`(valOps).get(anyString())

        val defaultEntries = mapOf("userId" to userEntity.userId.toString(), "occupyToken" to "test-token")
        doReturn(defaultEntries).`when`(hashOps).entries(anyString())

        concert = concertRepository.save(
            Concert.create("싸이 콘서트", "설명", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "poster.jpg")
        )
        val venue = venueRepository.save(Venue.create("공연장", "서울", 15000L))
        schedule = scheduleRepository.save(Schedule.create(concert, venue, LocalDateTime.now().plusHours(12), 1))

        seat = scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-1", 150000, HOLD))
        scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-2", 150000, HOLD))
        scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-3", 150000, HOLD))
        scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-4", 150000, HOLD))
    }

    @Test
    @DisplayName("티켓 2매 생성 성공")
    @Suppress("UNCHECKED_CAST")
    fun createTicket() {
        val mapAnswer = { invocation: org.mockito.invocation.InvocationOnMock ->
            val key = invocation.getArgument(0, String::class.java)
            when {
                key.endsWith("A-1") -> mapOf("userId" to userEntity.userId.toString(), "occupyToken" to "token-1")
                key.endsWith("A-2") -> mapOf("userId" to userEntity.userId.toString(), "occupyToken" to "token-2")
                else -> mapOf("userId" to userEntity.userId.toString(), "occupyToken" to "test-token")
            }
        }
        doAnswer(mapAnswer).`when`(hashOps).entries(anyString())

        val requestBody = """
            {
              "concertId": ${concert.concertId},
              "seatHolds": [
                {
                  "seatNumber": "A-1",
                  "occupyToken": "token-1"
                },
                {
                  "seatNumber": "A-2",
                  "occupyToken": "token-2"
                }
              ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/tickets/reserve/schedule/{scheduleId}", schedule.scheduleId)
                .header("X-Queue-Token", "test-queue-token")
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    @DisplayName("티켓 취소 성공")
    fun cancelTicket() {
        val savedTicket = ticketRepository.save(
            Ticket.create(userEntity, schedule, seat, "TICKET-12345", 150000, "GROUP-1")
        )

        mockMvc.perform(
            patch("/api/v1/tickets/cancel/{ticketId}", savedTicket.ticketId)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))

        val updatedTicket = ticketRepository.findById(savedTicket.ticketId!!).get()
        assertThat(updatedTicket.isValid).isFalse()
    }

    private fun saveUser(): User {
        return userRepository.save(
            User.create("user1", "user@test.com", "password", "테스트유저", LoginType.NORMAL)
        )
    }
}
