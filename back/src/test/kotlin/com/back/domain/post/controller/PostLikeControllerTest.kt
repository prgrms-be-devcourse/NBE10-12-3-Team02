package com.back.domain.post.controller

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.PostLike
import com.back.domain.post.entity.ReviewType
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.post.repository.PostLikeRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
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
class PostLikeControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val concertRepository: ConcertRepository,
    private val concertPostRepository: ConcertPostRepository,
    private val postLikeRepository: PostLikeRepository,
) {
    private lateinit var userEntity: User
    private lateinit var securityUser: SecurityUser
    private lateinit var concert: Concert
    private lateinit var post: ConcertPost

    @MockitoBean
    private lateinit var redissonClient: RedissonClient

    @BeforeEach
    @Suppress("UNCHECKED_CAST")
    fun setUp() {
        val activeSet = mock(RScoredSortedSet::class.java) as RScoredSortedSet<String>
        doReturn(activeSet).`when`(redissonClient)
            .getScoredSortedSet<String>(anyString(), any(Codec::class.java))
        `when`(activeSet.getScore(anyString()))
            .thenReturn((System.currentTimeMillis() + 600000).toDouble())

        val tokenBucket = mock(RBucket::class.java) as RBucket<String>
        doReturn(tokenBucket).`when`(redissonClient)
            .getBucket<String>(anyString(), any(Codec::class.java))
        `when`(tokenBucket.get()).thenReturn("test-queue-token")

        userEntity = userRepository.save(
            User.create(
                "like-user",
                "like-user@test.com",
                "0000",
                "좋아요사용자",
                LoginType.NORMAL,
            )
        )
        securityUser = SecurityUser(userEntity.userId!!, userEntity.name)
        concert = concertRepository.save(
            Concert.create(
                "좋아요 테스트 콘서트",
                "설명",
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(1),
                "poster.jpg",
            )
        )
        post = concertPostRepository.save(
            ConcertPost.create(concert, userEntity, "좋아요 테스트 게시글", "게시글 내용", rating = 5, reviewType = ReviewType.REVIEW)
        )
    }

    @Test
    @DisplayName("게시글 좋아요 등록 성공")
    fun likePost() {
        mockMvc.perform(
            put("/api/v1/posts/{postId}/likes", post.postId)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.isLiked").value(true))
            .andExpect(jsonPath("$.data.likeCount").value(1))

        assertThat(
            postLikeRepository.existsByPostPostIdAndUserUserId(
                post.postId!!,
                userEntity.userId!!,
            )
        ).isTrue
    }

    @Test
    @DisplayName("동일 게시글 좋아요를 반복 등록해도 한 건만 저장")
    fun likePostIdempotently() {
        repeat(2) {
            mockMvc.perform(
                put("/api/v1/posts/{postId}/likes", post.postId)
                    .with(user(securityUser))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1))
        }

        assertThat(postLikeRepository.countByPostPostId(post.postId!!)).isEqualTo(1)
    }

    @Test
    @DisplayName("게시글 좋아요 취소 성공")
    fun unlikePost() {
        postLikeRepository.saveAndFlush(PostLike.create(post, userEntity))

        mockMvc.perform(
            delete("/api/v1/posts/{postId}/likes", post.postId)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isLiked").value(false))
            .andExpect(jsonPath("$.data.likeCount").value(0))

        assertThat(postLikeRepository.countByPostPostId(post.postId!!)).isZero()
    }

    @Test
    @DisplayName("좋아요가 없는 게시글을 취소해도 성공")
    fun unlikePostIdempotently() {
        mockMvc.perform(
            delete("/api/v1/posts/{postId}/likes", post.postId)
                .with(user(securityUser))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isLiked").value(false))
            .andExpect(jsonPath("$.data.likeCount").value(0))
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글 좋아요 등록 불가")
    fun likePostUnauthorized() {
        mockMvc.perform(
            put("/api/v1/posts/{postId}/likes", post.postId)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("존재하지 않는 게시글 좋아요 등록 실패")
    fun likePostNotFound() {
        mockMvc.perform(
            put("/api/v1/posts/{postId}/likes", Long.MAX_VALUE)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-9"))
    }

    @Test
    @DisplayName("게시글 조회 시 좋아요 개수와 로그인 사용자의 좋아요 여부 반환")
    fun getPostWithLikeStatus() {
        val otherUser = userRepository.save(
            User.create(
                "other-like-user",
                "other-like-user@test.com",
                "0000",
                "다른사용자",
                LoginType.NORMAL,
            )
        )
        postLikeRepository.saveAllAndFlush(
            listOf(
                PostLike.create(post, userEntity),
                PostLike.create(post, otherUser),
            )
        )

        mockMvc.perform(
            get("/api/v1/posts/{postId}", post.postId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.likeCount").value(2))
            .andExpect(jsonPath("$.data.isLiked").value(true))
    }

    @Test
    @DisplayName("전체 게시글 목록에서 좋아요 개수와 로그인 사용자의 좋아요 여부 반환")
    fun getAllPostsWithLikeStatus() {
        val otherUser = userRepository.save(
            User.create(
                "list-like-user",
                "list-like-user@test.com",
                "0000",
                "목록사용자",
                LoginType.NORMAL,
            )
        )
        postLikeRepository.saveAllAndFlush(
            listOf(
                PostLike.create(post, userEntity),
                PostLike.create(post, otherUser),
            )
        )

        mockMvc.perform(
            get("/api/v1/posts")
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].likeCount").value(2))
            .andExpect(jsonPath("$.data[0].isLiked").value(true))
    }

    @Test
    @DisplayName("비로그인 게시글 조회 시 좋아요 개수는 반환하고 좋아요 여부는 false")
    fun getPostWithLikeStatusAnonymously() {
        postLikeRepository.saveAndFlush(PostLike.create(post, userEntity))

        mockMvc.perform(
            get("/api/v1/posts/{postId}", post.postId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.likeCount").value(1))
            .andExpect(jsonPath("$.data.isLiked").value(false))
    }

    @Test
    @DisplayName("게시글 삭제 시 연관 좋아요도 함께 삭제")
    fun deletePostWithLikes() {
        postLikeRepository.saveAndFlush(PostLike.create(post, userEntity))
        val postId = post.postId!!

        mockMvc.perform(
            delete(
                "/api/v1/concerts/{concertId}/posts/{postId}",
                concert.concertId,
                postId,
            )
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)

        assertThat(concertPostRepository.existsById(postId)).isFalse
        assertThat(postLikeRepository.countByPostPostId(postId)).isZero()
    }
}
