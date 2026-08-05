package com.back.domain.post.service

import com.back.domain.post.event.ReviewCreatedEvent
import com.back.domain.post.repository.ConcertPostRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

// AFTER_COMMIT + @Async 시점엔 이미 HTTP 응답이 나간 뒤라 실패를 유저에게 전달할 방법이 없다.
// 그래서 이 클래스만 컨벤션인 ErrorCode/ServiceException을 쓰지 않고, 실패 시 로그만 남기고
// summary를 null로 둔 채 조용히 넘어간다.
@Service
class ReviewSummaryService(
    private val concertPostRepository: ConcertPostRepository,
    private val reviewSummarizer: ReviewSummarizer,
    private val reviewSummaryCommandService: ReviewSummaryCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onReviewCreated(event: ReviewCreatedEvent) {
        val post = concertPostRepository.findById(event.postId).orElse(null) ?: return

        val summary = try {
            reviewSummarizer.summarize(post.content)
        } catch (e: Exception) {
            log.warn("리뷰 요약 생성 실패: postId={}", event.postId, e)
            return
        }

        reviewSummaryCommandService.saveSummary(event.postId, summary)
    }
}
