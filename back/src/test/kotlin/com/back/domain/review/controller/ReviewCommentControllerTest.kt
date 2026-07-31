package com.back.domain.review.controller

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.review.entity.ConcertReview
import com.back.domain.review.entity.ReviewComment
import com.back.domain.review.repository.ConcertReviewRepository
import com.back.domain.review.repository.ReviewCommentRepository
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
class ReviewCommentControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val concertRepository: ConcertRepository,
    private val venueRepository: VenueRepository,
    private val scheduleRepository: ScheduleRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val ticketRepository: TicketRepository,
    private val concertReviewRepository: ConcertReviewRepository,
    private val reviewCommentRepository: ReviewCommentRepository
) {
    private lateinit var userEntity: User
    private lateinit var securityUser: SecurityUser
    private lateinit var concert: Concert
    private lateinit var review: ConcertReview

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

        userEntity = userRepository.save(User.create("commenter1", "commenter1@test.com", "0000", "댓글러", LoginType.NORMAL))
        securityUser = SecurityUser(userEntity.userId!!, userEntity.name)

        concert = concertRepository.save(
            Concert.create("테스트 콘서트", "설명", LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1), "poster.jpg")
        )
        val venue = venueRepository.save(Venue.create("공연장", "서울", 1000L))
        val schedule = scheduleRepository.save(Schedule.create(concert, venue, LocalDateTime.now().minusDays(10), 1))
        val seat = scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-1", 150000, HOLD))
        seat.sell()
        ticketRepository.save(Ticket.create(userEntity, schedule, seat, "ticket-comment-001", 150000, "group-comment-001"))

        val author = userRepository.save(User.create("reviewer", "reviewer@test.com", "0000", "리뷰어", LoginType.NORMAL))
        review = concertReviewRepository.save(ConcertReview.create(concert, author, "좋은 공연", "감동이었어요."))
    }

    @Test
    @DisplayName("댓글 작성 성공")
    fun createComment() {
        val body = """{"content": "저도 같은 생각이에요!"}"""

        mockMvc.perform(
            post("/api/v1/reviews/{reviewId}/comments", review.reviewId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.data.content").value("저도 같은 생각이에요!"))
            .andExpect(jsonPath("$.data.isMine").value(true))
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 (비로그인)")
    fun getComments() {
        reviewCommentRepository.save(ReviewComment.create(review, userEntity, "첫 번째 댓글"))

        mockMvc.perform(
            get("/api/v1/reviews/{reviewId}/comments", review.reviewId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].content").value("첫 번째 댓글"))
            .andExpect(jsonPath("$.data[0].isMine").value(false))
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    fun deleteComment() {
        val comment = reviewCommentRepository.save(ReviewComment.create(review, userEntity, "삭제할 댓글"))

        mockMvc.perform(
            delete("/api/v1/reviews/{reviewId}/comments/{commentId}", review.reviewId, comment.commentId)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))

        assertThat(reviewCommentRepository.existsById(comment.commentId!!)).isFalse
    }

    @Test
    @DisplayName("댓글 삭제 실패 - URL의 리뷰와 댓글의 리뷰가 다름")
    fun deleteCommentWithMismatchedReview() {
        val comment = reviewCommentRepository.save(
            ReviewComment.create(review, userEntity, "삭제되지 않아야 하는 댓글")
        )
        val otherReview = concertReviewRepository.save(
            ConcertReview.create(concert, userEntity, "다른 리뷰", "다른 리뷰 내용")
        )

        mockMvc.perform(
            delete(
                "/api/v1/reviews/{reviewId}/comments/{commentId}",
                otherReview.reviewId,
                comment.commentId,
            )
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-10"))

        assertThat(reviewCommentRepository.existsById(comment.commentId!!)).isTrue
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 본인 댓글 아님")
    fun deleteCommentForbidden() {
        val otherUser = userRepository.save(User.create("other", "other@test.com", "0000", "타인", LoginType.NORMAL))
        val comment = reviewCommentRepository.save(ReviewComment.create(review, otherUser, "남의 댓글"))

        mockMvc.perform(
            delete("/api/v1/reviews/{reviewId}/comments/{commentId}", review.reviewId, comment.commentId)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-6"))

        assertThat(reviewCommentRepository.existsById(comment.commentId!!)).isTrue
    }
}
