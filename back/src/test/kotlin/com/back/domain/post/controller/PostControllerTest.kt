package com.back.domain.post.controller

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.ReviewType
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.schedule.entity.Schedule
import com.back.domain.schedule.entity.ScheduleSeat
import com.back.domain.schedule.constant.SeatStatus.HOLD
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.redisson.api.RBucket
import org.redisson.api.RScoredSortedSet
import org.redisson.api.RedissonClient
import org.redisson.client.codec.Codec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val concertRepository: ConcertRepository,
    private val venueRepository: VenueRepository,
    private val scheduleRepository: ScheduleRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val ticketRepository: TicketRepository,
    private val concertPostRepository: ConcertPostRepository
) {
    private lateinit var userEntity: User
    private lateinit var securityUser: SecurityUser
    private lateinit var concert: Concert
    private lateinit var schedule: Schedule
    private lateinit var venue: Venue

    @MockitoBean
    private lateinit var redissonClient: RedissonClient

    @BeforeEach
    @Suppress("UNCHECKED_CAST")
    fun setUp() {
        val activeSet = mock(RScoredSortedSet::class.java) as RScoredSortedSet<String>
        doReturn(activeSet).`when`(redissonClient).getScoredSortedSet<String>(anyString(), any(Codec::class.java))
        `when`(activeSet.getScore(anyString())).thenReturn((System.currentTimeMillis() + 600000).toDouble())

        val tokenBucket = mock(RBucket::class.java) as RBucket<String>
        doReturn(tokenBucket).`when`(redissonClient).getBucket<String>(anyString(), any(Codec::class.java))
        `when`(tokenBucket.get()).thenReturn("test-queue-token")

        userEntity = userRepository.save(User.create("poster1", "poster1@test.com", "0000", "게시글어", LoginType.NORMAL))
        securityUser = SecurityUser(userEntity.userId!!, userEntity.name)

        concert = concertRepository.save(
            Concert.create("테스트 콘서트", "설명", LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1), "poster.jpg")
        )
        venue = venueRepository.save(Venue.create("공연장", "서울", 1000L))
        schedule = scheduleRepository.save(
            Schedule.create(concert, venue, LocalDateTime.now().minusDays(10), 1)
        )
        val seat = scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-1", 150000, HOLD))
        seat.sell()
        ticketRepository.save(Ticket.create(userEntity, schedule, seat, "ticket-post-board-001", 150000, "group-post-board-001"))
    }

    @Test
    @DisplayName("전체 게시글 피드 조회 성공 (비로그인)")
    fun getAllPosts() {
        concertPostRepository.save(ConcertPost.create(concert, userEntity, "좋은 공연", "감동이었어요.", rating = 5, reviewType = ReviewType.REVIEW))

        mockMvc.perform(
            get("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].title").value("좋은 공연"))
            .andExpect(jsonPath("$.data[0].concertName").value("테스트 콘서트"))
            .andExpect(jsonPath("$.data[0].isMine").value(false))
    }

    @Test
    @DisplayName("전체 게시글 피드 조회 - 로그인 시 isMine 반영")
    fun getAllPostsLoggedIn() {
        concertPostRepository.save(ConcertPost.create(concert, userEntity, "내 게시글", "내가 쓴 게시글입니다.", rating = 5, reviewType = ReviewType.REVIEW))

        mockMvc.perform(
            get("/api/v1/posts")
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].isMine").value(true))
    }

    @Test
    @DisplayName("자격 있는 콘서트 목록 조회 성공")
    fun getEligibleConcerts() {
        mockMvc.perform(
            get("/api/v1/posts/eligible-concerts")
                .param("reviewType", "REVIEW")
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].concertTitle").value("테스트 콘서트"))
    }

    @Test
    @DisplayName("자격 있는 콘서트 목록 - 이미 게시글 쓴 콘서트는 제외")
    fun getEligibleConcertsExcludesAlreadyPosted() {
        concertPostRepository.save(ConcertPost.create(concert, userEntity, "이미 쓴 게시글", "내용", rating = 5, reviewType = ReviewType.REVIEW))

        mockMvc.perform(
            get("/api/v1/posts/eligible-concerts")
                .param("reviewType", "REVIEW")
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    @DisplayName("자격 있는 콘서트 목록 - 6개월 초과 공연은 제외")
    fun getEligibleConcertsExcludesExpired() {
        val oldConcert = concertRepository.save(
            Concert.create("오래된 콘서트", "설명", LocalDateTime.now().minusYears(2), LocalDateTime.now().minusYears(2).plusDays(1), "old.jpg")
        )
        val oldSchedule = scheduleRepository.save(
            Schedule.create(oldConcert, venue, LocalDateTime.now().minusMonths(7), 1)
        )
        val oldSeat = scheduleSeatRepository.save(ScheduleSeat.create(oldSchedule, "VIP", "B-1", 150000, HOLD))
        oldSeat.sell()
        ticketRepository.save(Ticket.create(userEntity, oldSchedule, oldSeat, "old-board-ticket-001", 150000, "old-board-group-001"))

        mockMvc.perform(
            get("/api/v1/posts/eligible-concerts")
                .param("reviewType", "REVIEW")
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[?(@.concertTitle == '오래된 콘서트')]").isEmpty)
    }

    @Test
    @DisplayName("자격 있는 콘서트 목록 - 티켓 없는 사용자는 빈 목록")
    fun getEligibleConcertsNoTicket() {
        val otherUser = userRepository.save(User.create("noticket", "noticket@board.com", "0000", "무티켓", LoginType.NORMAL))

        mockMvc.perform(
            get("/api/v1/posts/eligible-concerts")
                .param("reviewType", "REVIEW")
                .with(user(SecurityUser(otherUser.userId!!, otherUser.name)))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    @DisplayName("자격 있는 콘서트 목록 - 비로그인 시 401")
    fun getEligibleConcertsUnauthorized() {
        mockMvc.perform(
            get("/api/v1/posts/eligible-concerts")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized)
    }
}
