package com.back.domain.post.controller

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.PostBookmark
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.post.repository.PostBookmarkRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostBookmarkControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val concertRepository: ConcertRepository,
    private val concertPostRepository: ConcertPostRepository,
    private val postBookmarkRepository: PostBookmarkRepository,
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
        post = concertPostRepository.save(
            ConcertPost.create(concert, userEntity, "북마크 테스트 게시글", "게시글 내용")
        )
    }

    @Test
    @DisplayName("게시글 북마크 등록은 반복 요청에도 한 건만 저장된다")
    fun t1() {
        repeat(2) {
            mockMvc.perform(
                put("/api/v1/posts/{postId}/bookmarks", post.postId)
                    .with(user(securityUser))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.isBookmarked").value(true))
        }

        assertThat(postBookmarkRepository.count()).isEqualTo(1)
    }

    @Test
    @DisplayName("게시글 북마크 취소는 북마크가 없어도 성공한다")
    fun t2() {
        mockMvc.perform(
            delete("/api/v1/posts/{postId}/bookmarks", post.postId)
                .with(user(securityUser))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isBookmarked").value(false))
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글을 북마크할 수 없다")
    fun t3() {
        mockMvc.perform(
            put("/api/v1/posts/{postId}/bookmarks", post.postId)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 북마크할 수 없다")
    fun t4() {
        mockMvc.perform(
            put("/api/v1/posts/{postId}/bookmarks", Long.MAX_VALUE)
                .with(user(securityUser))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-9"))
    }

    @Test
    @DisplayName("게시글 조회는 현재 사용자의 북마크 여부를 반환한다")
    fun t5() {
        postBookmarkRepository.saveAndFlush(PostBookmark.create(post, userEntity))

        mockMvc.perform(
            get("/api/v1/posts/{postId}", post.postId)
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isBookmarked").value(true))

        mockMvc.perform(
            get("/api/v1/posts/{postId}", post.postId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isBookmarked").value(false))
    }

    @Test
    @DisplayName("내 북마크 목록에는 내가 북마크한 게시글만 반환된다")
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
        postBookmarkRepository.saveAllAndFlush(
            listOf(
                PostBookmark.create(post, userEntity),
                PostBookmark.create(post, otherUser),
            )
        )

        mockMvc.perform(
            get("/api/v1/users/me/post-bookmarks")
                .with(user(securityUser))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].postId").value(post.postId))
            .andExpect(jsonPath("$.data.content[0].title").value(post.title))
    }

    @Test
    @DisplayName("게시글 삭제 시 연관 북마크도 삭제된다")
    fun t7() {
        postBookmarkRepository.saveAndFlush(PostBookmark.create(post, userEntity))
        val postId = post.postId!!

        mockMvc.perform(
            delete(
                "/api/v1/concerts/{concertId}/posts/{postId}",
                concert.concertId,
                postId,
            )
                .with(user(securityUser))
        )
            .andExpect(status().isOk)

        assertThat(concertPostRepository.existsById(postId)).isFalse
        assertThat(postBookmarkRepository.count()).isZero()
    }
}
