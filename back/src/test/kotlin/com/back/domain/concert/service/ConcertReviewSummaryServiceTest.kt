package com.back.domain.concert.service

import com.back.domain.concert.entity.Concert
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.ReviewType
import com.back.domain.post.event.ConcertReviewsUpdatedEvent
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class ConcertReviewSummaryServiceTest {

    private fun user(): User =
        User.create("author", "author@test.com", "0000", "작성자", LoginType.NORMAL)
            .also { ReflectionTestUtils.setField(it, "userId", 1L) }

    private fun concert(): Concert =
        Concert.create("콘서트", "설명", LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1), null)
            .also { ReflectionTestUtils.setField(it, "concertId", 1L) }

    private fun review(id: Long, content: String): ConcertPost =
        ConcertPost.create(concert(), user(), "제목", content, rating = 5, reviewType = ReviewType.REVIEW)
            .also { ReflectionTestUtils.setField(it, "postId", id) }

    @Test
    @DisplayName("REVIEW가 3건 미만이면 요약을 시도하지 않는다")
    fun t1() {
        val concertPostRepository = mock(ConcertPostRepository::class.java)
        val concertReviewSummarizer = mock(ConcertReviewSummarizer::class.java)
        val concertReviewSummaryCommandService = mock(ConcertReviewSummaryCommandService::class.java)
        val service = ConcertReviewSummaryService(concertPostRepository, concertReviewSummarizer, concertReviewSummaryCommandService)

        `when`(concertPostRepository.countByConcert_ConcertIdAndReviewType(1L, ReviewType.REVIEW)).thenReturn(2L)

        service.onReviewsUpdated(ConcertReviewsUpdatedEvent(1L))

        verify(concertPostRepository, never())
            .findTop20ByConcert_ConcertIdAndReviewTypeOrderByCreateDateDesc(1L, ReviewType.REVIEW)
        verifyNoInteractions(concertReviewSummarizer)
        verifyNoInteractions(concertReviewSummaryCommandService)
    }

    @Test
    @DisplayName("REVIEW가 3건 이상이면 최근 리뷰를 모아 요약하고 저장한다")
    fun t2() {
        val concertPostRepository = mock(ConcertPostRepository::class.java)
        val concertReviewSummarizer = mock(ConcertReviewSummarizer::class.java)
        val concertReviewSummaryCommandService = mock(ConcertReviewSummaryCommandService::class.java)
        val service = ConcertReviewSummaryService(concertPostRepository, concertReviewSummarizer, concertReviewSummaryCommandService)

        val reviews = listOf(review(1L, "좋았다"), review(2L, "별로였다"), review(3L, "최고였다"))
        `when`(concertPostRepository.countByConcert_ConcertIdAndReviewType(1L, ReviewType.REVIEW)).thenReturn(3L)
        `when`(concertPostRepository.findTop20ByConcert_ConcertIdAndReviewTypeOrderByCreateDateDesc(1L, ReviewType.REVIEW))
            .thenReturn(reviews)
        `when`(concertReviewSummarizer.summarize(listOf("좋았다", "별로였다", "최고였다")))
            .thenReturn("전반적으로 만족도가 높았다.")

        service.onReviewsUpdated(ConcertReviewsUpdatedEvent(1L))

        verify(concertReviewSummaryCommandService).saveSummary(1L, "전반적으로 만족도가 높았다.")
    }

    @Test
    @DisplayName("요약 생성이 실패하면 로그만 남기고 저장은 시도하지 않는다")
    fun t3() {
        val concertPostRepository = mock(ConcertPostRepository::class.java)
        val concertReviewSummarizer = mock(ConcertReviewSummarizer::class.java)
        val concertReviewSummaryCommandService = mock(ConcertReviewSummaryCommandService::class.java)
        val service = ConcertReviewSummaryService(concertPostRepository, concertReviewSummarizer, concertReviewSummaryCommandService)

        val reviews = listOf(review(1L, "좋았다"), review(2L, "별로였다"), review(3L, "최고였다"))
        `when`(concertPostRepository.countByConcert_ConcertIdAndReviewType(1L, ReviewType.REVIEW)).thenReturn(3L)
        `when`(concertPostRepository.findTop20ByConcert_ConcertIdAndReviewTypeOrderByCreateDateDesc(1L, ReviewType.REVIEW))
            .thenReturn(reviews)
        `when`(concertReviewSummarizer.summarize(listOf("좋았다", "별로였다", "최고였다")))
            .thenThrow(RuntimeException("OpenAI 호출 실패"))

        service.onReviewsUpdated(ConcertReviewsUpdatedEvent(1L))

        verifyNoInteractions(concertReviewSummaryCommandService)
    }
}
