package com.back.domain.post.service

import com.back.domain.concert.entity.Concert
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.ReviewType
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class ConcertPostCommandServiceTest {

    private fun user(): User =
        User.create("author", "author@test.com", "0000", "작성자", LoginType.NORMAL)
            .also { ReflectionTestUtils.setField(it, "userId", 1L) }

    private fun concert(): Concert =
        Concert.create("콘서트", "설명", LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1), null)
            .also { ReflectionTestUtils.setField(it, "concertId", 1L) }

    @Test
    @DisplayName("저장된 엔티티를 그대로 반환한다")
    fun t1() {
        val repository = mock(ConcertPostRepository::class.java)
        val service = ConcertPostCommandService(repository)

        val post = ConcertPost.create(concert(), user(), "제목", "내용", rating = 5, reviewType = ReviewType.REVIEW)
        ReflectionTestUtils.setField(post, "postId", 12L)
        `when`(repository.saveAndFlush(post)).thenReturn(post)

        val result = service.save(post)

        assertThat(result).isSameAs(post)
    }
}
