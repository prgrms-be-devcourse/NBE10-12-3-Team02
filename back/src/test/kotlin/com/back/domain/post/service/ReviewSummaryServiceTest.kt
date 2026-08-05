package com.back.domain.post.service

import com.back.domain.concert.entity.Concert
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.ReviewType
import com.back.domain.post.event.ReviewCreatedEvent
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.Optional

class ReviewSummaryServiceTest {

    private fun user(): User =
        User.create("author", "author@test.com", "0000", "작성자", LoginType.NORMAL)
            .also { ReflectionTestUtils.setField(it, "userId", 1L) }

    private fun concert(): Concert =
        Concert.create("콘서트", "설명", LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1), null)
            .also { ReflectionTestUtils.setField(it, "concertId", 1L) }

    private fun post(id: Long): ConcertPost =
        ConcertPost.create(concert(), user(), "제목", "정말 좋았던 공연이었습니다", rating = 5, reviewType = ReviewType.REVIEW)
            .also { ReflectionTestUtils.setField(it, "postId", id) }

    @Test
    @DisplayName("정상 흐름: 게시글을 조회해 요약하고 저장한다")
    fun t1() {
        val concertPostRepository = mock(ConcertPostRepository::class.java)
        val reviewSummarizer = mock(ReviewSummarizer::class.java)
        val reviewSummaryCommandService = mock(ReviewSummaryCommandService::class.java)
        val service = ReviewSummaryService(concertPostRepository, reviewSummarizer, reviewSummaryCommandService)

        val post = post(10L)
        `when`(concertPostRepository.findById(10L)).thenReturn(Optional.of(post))
        `when`(reviewSummarizer.summarize(post.content)).thenReturn("좋았던 공연")

        service.onReviewCreated(ReviewCreatedEvent(10L))

        verify(reviewSummaryCommandService).saveSummary(10L, "좋았던 공연")
    }

    @Test
    @DisplayName("게시글이 존재하지 않으면 요약을 시도하지 않고 조용히 반환한다")
    fun t2() {
        val concertPostRepository = mock(ConcertPostRepository::class.java)
        val reviewSummarizer = mock(ReviewSummarizer::class.java)
        val reviewSummaryCommandService = mock(ReviewSummaryCommandService::class.java)
        val service = ReviewSummaryService(concertPostRepository, reviewSummarizer, reviewSummaryCommandService)

        `when`(concertPostRepository.findById(999L)).thenReturn(Optional.empty())

        service.onReviewCreated(ReviewCreatedEvent(999L))

        verify(reviewSummaryCommandService, never()).saveSummary(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    @DisplayName("요약 생성이 실패하면 로그만 남기고 저장은 시도하지 않는다")
    fun t3() {
        val concertPostRepository = mock(ConcertPostRepository::class.java)
        val reviewSummarizer = mock(ReviewSummarizer::class.java)
        val reviewSummaryCommandService = mock(ReviewSummaryCommandService::class.java)
        val service = ReviewSummaryService(concertPostRepository, reviewSummarizer, reviewSummaryCommandService)

        val post = post(11L)
        `when`(concertPostRepository.findById(11L)).thenReturn(Optional.of(post))
        `when`(reviewSummarizer.summarize(post.content)).thenThrow(RuntimeException("OpenAI 호출 실패"))

        service.onReviewCreated(ReviewCreatedEvent(11L))

        verify(reviewSummaryCommandService, never()).saveSummary(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString())
    }
}
