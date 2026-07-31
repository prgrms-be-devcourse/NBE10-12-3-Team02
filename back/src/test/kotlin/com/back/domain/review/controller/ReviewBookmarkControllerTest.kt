package com.back.domain.review.controller

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.review.entity.ConcertReview
import com.back.domain.review.entity.ReviewBookmark
import com.back.domain.review.repository.ConcertReviewRepository
import com.back.domain.review.repository.ReviewBookmarkRepository
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewBookmarkControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val concertRepository: ConcertRepository,
    private val concertReviewRepository: ConcertReviewRepository,
    private val reviewBookmarkRepository: ReviewBookmarkRepository,
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
                "bookmark-user",
                "bookmark-user@test.com",
                "0000",
                "북마크사용자",
                LoginType.NORMAL,
            )
        )
        securityUser = SecurityUser(userEntity.userId!!, userEntity.name)
        concert = concertRepository.save(
            Concert.create(
                "북마크 테스트 콘서트",
                "설명",
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(1),
                "poster.jpg",
            )
        )
        review = concertReviewRepository.save(
            ConcertReview.create(concert, userEntity, "북마크 테스트 리뷰", "리뷰 내용")
        )
    }

    @Test
    @DisplayName("리뷰 북마크 등록은 반복 요청에도 한 건만 저장된다")
    fun t1() {
        repeat(2) {
            mockMvc.perform(
                put("/api/v1/reviews/{reviewId}/bookmarks", review.reviewId)
                    .with(user(securityUser))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.isBookmarked").value(true))
        }

        assertThat(reviewBookmarkRepository.count()).isEqualTo(1)
    }

    @Test
    @DisplayName("리뷰 북마크 취소는 북마크가 없어도 성공한다")
    fun t2() {
        mockMvc.perform(
            delete("/api/v1/reviews/{reviewId}/bookmarks", review.reviewId)
                .with(user(securityUser))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isBookmarked").value(false))
    }

    @Test
    @DisplayName("비로그인 사용자는 리뷰를 북마크할 수 없다")
    fun t3() {
        mockMvc.perform(
            put("/api/v1/reviews/{reviewId}/bookmarks", review.reviewId)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("존재하지 않는 리뷰는 북마크할 수 없다")
    fun t4() {
        mockMvc.perform(
            put("/api/v1/reviews/{reviewId}/bookmarks", Long.MAX_VALUE)
                .with(user(securityUser))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-9"))
    }

    @Test
    @DisplayName("리뷰 조회는 현재 사용자의 북마크 여부를 반환한다")
    fun t5() {
        reviewBookmarkRepository.saveAndFlush(ReviewBookmark.create(review, userEntity))

        mockMvc.perform(
            get("/api/v1/reviews/{reviewId}", review.reviewId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isBookmarked").value(true))

        mockMvc.perform(
            get("/api/v1/reviews/{reviewId}", review.reviewId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isBookmarked").value(false))
    }

    @Test
    @DisplayName("내 북마크 목록에는 내가 북마크한 리뷰만 반환된다")
    fun t6() {
        val otherUser = userRepository.save(
            User.create(
                "other-bookmark-user",
                "other-bookmark-user@test.com",
                "0000",
                "다른사용자",
                LoginType.NORMAL,
            )
        )
        reviewBookmarkRepository.saveAllAndFlush(
            listOf(
                ReviewBookmark.create(review, userEntity),
                ReviewBookmark.create(review, otherUser),
            )
        )

        mockMvc.perform(
            get("/api/v1/users/me/review-bookmarks")
                .with(user(securityUser))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].reviewId").value(review.reviewId))
            .andExpect(jsonPath("$.data.content[0].title").value(review.title))
    }

    @Test
    @DisplayName("리뷰 삭제 시 연관 북마크도 삭제된다")
    fun t7() {
        reviewBookmarkRepository.saveAndFlush(ReviewBookmark.create(review, userEntity))
        val reviewId = review.reviewId!!

        mockMvc.perform(
            delete(
                "/api/v1/concerts/{concertId}/reviews/{reviewId}",
                concert.concertId,
                reviewId,
            )
                .with(user(securityUser))
        )
            .andExpect(status().isOk)

        assertThat(concertReviewRepository.existsById(reviewId)).isFalse
        assertThat(reviewBookmarkRepository.count()).isZero()
    }
}
