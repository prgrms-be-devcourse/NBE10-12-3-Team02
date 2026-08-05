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
import java.util.Optional

class ReviewSummaryCommandServiceTest {

    private fun user(): User =
        User.create("author", "author@test.com", "0000", "작성자", LoginType.NORMAL)
            .also { ReflectionTestUtils.setField(it, "userId", 1L) }

    private fun concert(): Concert =
        Concert.create("콘서트", "설명", LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1), null)
            .also { ReflectionTestUtils.setField(it, "concertId", 1L) }

    private fun post(id: Long): ConcertPost =
        ConcertPost.create(concert(), user(), "제목", "내용", rating = 5, reviewType = ReviewType.REVIEW)
            .also { ReflectionTestUtils.setField(it, "postId", id) }

    @Test
    @DisplayName("게시글이 존재하면 요약을 저장한다")
    fun t1() {
        val repository = mock(ConcertPostRepository::class.java)
        val service = ReviewSummaryCommandService(repository)
        val post = post(10L)
        `when`(repository.findById(10L)).thenReturn(Optional.of(post))

        service.saveSummary(10L, "좋았던 공연")

        assertThat(post.summary).isEqualTo("좋았던 공연")
    }

    @Test
    @DisplayName("게시글이 존재하지 않으면 아무 것도 하지 않는다")
    fun t2() {
        val repository = mock(ConcertPostRepository::class.java)
        val service = ReviewSummaryCommandService(repository)
        `when`(repository.findById(999L)).thenReturn(Optional.empty())

        service.saveSummary(999L, "좋았던 공연")
        // 예외 없이 조용히 반환되면 성공
    }
}
