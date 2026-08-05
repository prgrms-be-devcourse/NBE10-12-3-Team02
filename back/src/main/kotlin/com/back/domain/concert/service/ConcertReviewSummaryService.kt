package com.back.domain.concert.service

import com.back.domain.post.entity.ReviewType
import com.back.domain.post.event.ConcertReviewsUpdatedEvent
import com.back.domain.post.repository.ConcertPostRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

// AFTER_COMMIT + @Async 시점엔 이미 응답이 나간 뒤라 에러를 유저에게 전달할 방법이 없어,
// 이 클래스만 컨벤션(ErrorCode/ServiceException)을 쓰지 않고 실패 시 로그만 남기고 조용히 넘어간다.
@Service
class ConcertReviewSummaryService(
    private val concertPostRepository: ConcertPostRepository,
    private val concertReviewSummarizer: ConcertReviewSummarizer,
    private val concertReviewSummaryCommandService: ConcertReviewSummaryCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onReviewsUpdated(event: ConcertReviewsUpdatedEvent) {
        val reviewCount = concertPostRepository.countByConcert_ConcertIdAndReviewType(event.concertId, ReviewType.REVIEW)
        if (reviewCount < MIN_REVIEW_COUNT) return

        val reviews = concertPostRepository
            .findTop20ByConcert_ConcertIdAndReviewTypeOrderByCreateDateDesc(event.concertId, ReviewType.REVIEW)
            .map { it.content }

        val summary = try {
            concertReviewSummarizer.summarize(reviews)
        } catch (e: Exception) {
            log.warn("콘서트 종합 요약 생성 실패: concertId={}", event.concertId, e)
            return
        }

        concertReviewSummaryCommandService.saveSummary(event.concertId, summary)
    }

    companion object {
        private const val MIN_REVIEW_COUNT = 3
    }
}
