package com.back.domain.review.controller

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.review.entity.ConcertReview
import com.back.domain.review.repository.ConcertReviewRepository
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
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConcertReviewControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val concertRepository: ConcertRepository,
    private val venueRepository: VenueRepository,
    private val scheduleRepository: ScheduleRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val ticketRepository: TicketRepository,
    private val concertReviewRepository: ConcertReviewRepository
) {
    private lateinit var userEntity: User
    private lateinit var securityUser: SecurityUser
    private lateinit var concert: Concert
    private lateinit var schedule: Schedule
    private lateinit var seat: ScheduleSeat

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

        userEntity = userRepository.save(User.create("reviewer1", "reviewer1@test.com", "0000", "리뷰어", LoginType.NORMAL))
        securityUser = SecurityUser(userEntity.userId!!, userEntity.name)

        concert = concertRepository.save(
            Concert.create("테스트 콘서트", "설명", LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1), "poster.jpg")
        )
        val venue = venueRepository.save(Venue.create("공연장", "서울", 1000L))
        schedule = scheduleRepository.save(
            Schedule.create(concert, venue, LocalDateTime.now().minusDays(10), 1)
        )
        seat = scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-1", 150000, HOLD))
        seat.sell()
        ticketRepository.save(Ticket.create(userEntity, schedule, seat, "ticket-review-001", 150000, "group-review-001"))
    }

    @Test
    @DisplayName("리뷰 작성 성공")
    fun createReview() {
        val requestBody = """
            {
              "title": "정말 좋은 공연이었어요",
              "content": "감동적인 무대였습니다. 다음에도 꼭 보러 가고 싶네요."
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/concerts/{concertId}/reviews", concert.concertId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.msg").value("리뷰가 작성되었습니다."))
            .andExpect(jsonPath("$.data.title").value("정말 좋은 공연이었어요"))
            .andExpect(jsonPath("$.data.isMine").value(true))
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 유효한 티켓 없음")
    fun createReviewNoTicket() {
        val otherUser = userRepository.save(User.create("noticket", "noticket@test.com", "0000", "무티켓", LoginType.NORMAL))
        val otherSecurityUser = SecurityUser(otherUser.userId!!, otherUser.name)

        val requestBody = """
            {
              "title": "제목",
              "content": "내용"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/concerts/{concertId}/reviews", concert.concertId)
                .with(user(otherSecurityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-4"))
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 6개월 초과")
    fun createReviewPeriodExpired() {
        val oldConcert = concertRepository.save(
            Concert.create("오래된 콘서트", "설명", LocalDateTime.now().minusYears(2), LocalDateTime.now().minusYears(2).plusDays(1), "poster.jpg")
        )
        val oldSchedule = scheduleRepository.save(
            Schedule.create(oldConcert, schedule.venue, LocalDateTime.now().minusMonths(7), 1)
        )
        val oldSeat = scheduleSeatRepository.save(ScheduleSeat.create(oldSchedule, "VIP", "A-1", 150000, HOLD))
        oldSeat.sell()
        ticketRepository.save(Ticket.create(userEntity, oldSchedule, oldSeat, "old-ticket-001", 150000, "old-group-001"))

        val requestBody = """
            {
              "title": "제목",
              "content": "내용"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/concerts/{concertId}/reviews", oldConcert.concertId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-5"))
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 (비로그인)")
    fun getReviews() {
        val review = concertReviewRepository.save(
            ConcertReview.create(concert, userEntity, "좋았어요", "좋은 공연이었습니다.")
        )

        mockMvc.perform(
            get("/api/v1/concerts/{concertId}/reviews", concert.concertId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].reviewId").value(review.reviewId))
            .andExpect(jsonPath("$.data[0].isMine").value(false))
    }

    @Test
    @DisplayName("리뷰 상세 조회 성공")
    fun getReview() {
        val review = concertReviewRepository.save(
            ConcertReview.create(concert, userEntity, "좋았어요", "좋은 공연이었습니다.")
        )

        mockMvc.perform(
            get("/api/v1/concerts/{concertId}/reviews/{reviewId}", concert.concertId, review.reviewId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.title").value("좋았어요"))
            .andExpect(jsonPath("$.data.isMine").value(true))
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    fun updateReview() {
        val review = concertReviewRepository.save(
            ConcertReview.create(concert, userEntity, "제목", "내용")
        )

        val requestBody = """
            {
              "title": "수정된 제목",
              "content": "수정된 내용입니다."
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/concerts/{concertId}/reviews/{reviewId}", concert.concertId, review.reviewId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.title").value("수정된 제목"))
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 권한 없음")
    fun updateReviewForbidden() {
        val otherUser = userRepository.save(User.create("other2", "other2@test.com", "0000", "타인", LoginType.NORMAL))
        val review = concertReviewRepository.save(
            ConcertReview.create(concert, userEntity, "제목", "내용")
        )

        val requestBody = """
            {
              "title": "해킹 제목",
              "content": "해킹 내용"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/concerts/{concertId}/reviews/{reviewId}", concert.concertId, review.reviewId)
                .with(user(SecurityUser(otherUser.userId!!, otherUser.name)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-2"))
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    fun deleteReview() {
        val review = concertReviewRepository.save(
            ConcertReview.create(concert, userEntity, "제목", "내용")
        )

        mockMvc.perform(
            delete("/api/v1/concerts/{concertId}/reviews/{reviewId}", concert.concertId, review.reviewId)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("리뷰가 삭제되었습니다."))

        assertThat(concertReviewRepository.existsById(review.reviewId!!)).isFalse
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 권한 없음")
    fun deleteReviewForbidden() {
        val otherUser = userRepository.save(User.create("other3", "other3@test.com", "0000", "타인3", LoginType.NORMAL))
        val review = concertReviewRepository.save(
            ConcertReview.create(concert, userEntity, "제목", "내용")
        )

        mockMvc.perform(
            delete("/api/v1/concerts/{concertId}/reviews/{reviewId}", concert.concertId, review.reviewId)
                .with(user(SecurityUser(otherUser.userId!!, otherUser.name)))
        )
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-2"))

        assertThat(concertReviewRepository.existsById(review.reviewId!!)).isTrue
    }

    @Test
    @DisplayName("리뷰 중복 작성 실패")
    fun createDuplicateReview() {
        concertReviewRepository.save(
            ConcertReview.create(concert, userEntity, "첫 번째 리뷰", "내용")
        )

        val requestBody = """
            {
              "title": "두 번째 리뷰",
              "content": "내용"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/concerts/{concertId}/reviews", concert.concertId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409-4"))
    }
}
