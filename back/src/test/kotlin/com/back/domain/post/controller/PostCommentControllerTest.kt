package com.back.domain.post.controller

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.PostComment
import com.back.domain.post.entity.ReviewType
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.post.repository.PostCommentRepository
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
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostCommentControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val concertRepository: ConcertRepository,
    private val venueRepository: VenueRepository,
    private val scheduleRepository: ScheduleRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val ticketRepository: TicketRepository,
    private val concertPostRepository: ConcertPostRepository,
    private val postCommentRepository: PostCommentRepository
) {
    private lateinit var userEntity: User
    private lateinit var securityUser: SecurityUser
    private lateinit var concert: Concert
    private lateinit var post: ConcertPost

    @MockitoBean
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @BeforeEach
    @Suppress("UNCHECKED_CAST")
    fun setUp() {
        val zSetOps = mock(ZSetOperations::class.java) as ZSetOperations<String, String>
        val valOps = mock(ValueOperations::class.java) as ValueOperations<String, String>
        doReturn(zSetOps).`when`(stringRedisTemplate).opsForZSet()
        doReturn(valOps).`when`(stringRedisTemplate).opsForValue()
        `when`(zSetOps.score(anyString(), anyString())).thenReturn((System.currentTimeMillis() + 600000).toDouble())
        `when`(valOps.get(anyString())).thenReturn("test-queue-token")

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

        val author = userRepository.save(User.create("poster", "poster@test.com", "0000", "게시글어", LoginType.NORMAL))
        post = concertPostRepository.save(ConcertPost.create(concert, author, "좋은 공연", "감동이었어요.", rating = 5, reviewType = ReviewType.REVIEW))
    }

    @Test
    @DisplayName("댓글 작성 성공")
    fun createComment() {
        val body = """{"content": "저도 같은 생각이에요!"}"""

        mockMvc.perform(
            post("/api/v1/posts/{postId}/comments", post.postId)
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
        postCommentRepository.save(PostComment.create(post, userEntity, "첫 번째 댓글"))

        mockMvc.perform(
            get("/api/v1/posts/{postId}/comments", post.postId)
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
    @DisplayName("댓글 수정 성공")
    fun updateComment() {
        val comment = postCommentRepository.save(PostComment.create(post, userEntity, "수정 전 댓글"))
        val body = """{"content": "수정 후 댓글"}"""

        mockMvc.perform(
            put("/api/v1/posts/{postId}/comments/{commentId}", post.postId, comment.commentId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.content").value("수정 후 댓글"))
            .andExpect(jsonPath("$.data.isMine").value(true))

        assertThat(postCommentRepository.findById(comment.commentId!!).get().content).isEqualTo("수정 후 댓글")
    }

    @Test
    @DisplayName("댓글 수정 실패 - URL의 게시글과 댓글의 게시글이 다름")
    fun updateCommentWithMismatchedPost() {
        val comment = postCommentRepository.save(
            PostComment.create(post, userEntity, "수정되지 않아야 하는 댓글")
        )
        val otherPost = concertPostRepository.save(
            ConcertPost.create(concert, userEntity, "다른 게시글", "다른 게시글 내용", rating = 5, reviewType = ReviewType.REVIEW)
        )
        val body = """{"content": "수정 시도"}"""

        mockMvc.perform(
            put(
                "/api/v1/posts/{postId}/comments/{commentId}",
                otherPost.postId,
                comment.commentId,
            )
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-10"))

        assertThat(postCommentRepository.findById(comment.commentId!!).get().content).isEqualTo("수정되지 않아야 하는 댓글")
    }

    @Test
    @DisplayName("댓글 수정 실패 - 본인 댓글 아님")
    fun updateCommentForbidden() {
        val otherUser = userRepository.save(User.create("other-editor", "other-editor@test.com", "0000", "타인", LoginType.NORMAL))
        val comment = postCommentRepository.save(PostComment.create(post, otherUser, "남의 댓글"))
        val body = """{"content": "수정 시도"}"""

        mockMvc.perform(
            put("/api/v1/posts/{postId}/comments/{commentId}", post.postId, comment.commentId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-6"))

        assertThat(postCommentRepository.findById(comment.commentId!!).get().content).isEqualTo("남의 댓글")
    }

    @Test
    @DisplayName("댓글 수정 실패 - 빈 내용")
    fun updateCommentBlank() {
        val comment = postCommentRepository.save(PostComment.create(post, userEntity, "원본 댓글"))
        val body = """{"content": ""}"""

        mockMvc.perform(
            put("/api/v1/posts/{postId}/comments/{commentId}", post.postId, comment.commentId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andDo(print())
            .andExpect(status().isBadRequest)

        assertThat(postCommentRepository.findById(comment.commentId!!).get().content).isEqualTo("원본 댓글")
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    fun deleteComment() {
        val comment = postCommentRepository.save(PostComment.create(post, userEntity, "삭제할 댓글"))

        mockMvc.perform(
            delete("/api/v1/posts/{postId}/comments/{commentId}", post.postId, comment.commentId)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))

        assertThat(postCommentRepository.existsById(comment.commentId!!)).isFalse
    }

    @Test
    @DisplayName("댓글 삭제 실패 - URL의 게시글과 댓글의 게시글이 다름")
    fun deleteCommentWithMismatchedPost() {
        val comment = postCommentRepository.save(
            PostComment.create(post, userEntity, "삭제되지 않아야 하는 댓글")
        )
        val otherPost = concertPostRepository.save(
            ConcertPost.create(concert, userEntity, "다른 게시글", "다른 게시글 내용", rating = 5, reviewType = ReviewType.REVIEW)
        )

        mockMvc.perform(
            delete(
                "/api/v1/posts/{postId}/comments/{commentId}",
                otherPost.postId,
                comment.commentId,
            )
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-10"))

        assertThat(postCommentRepository.existsById(comment.commentId!!)).isTrue
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 본인 댓글 아님")
    fun deleteCommentForbidden() {
        val otherUser = userRepository.save(User.create("other", "other@test.com", "0000", "타인", LoginType.NORMAL))
        val comment = postCommentRepository.save(PostComment.create(post, otherUser, "남의 댓글"))

        mockMvc.perform(
            delete("/api/v1/posts/{postId}/comments/{commentId}", post.postId, comment.commentId)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-6"))

        assertThat(postCommentRepository.existsById(comment.commentId!!)).isTrue
    }
}
