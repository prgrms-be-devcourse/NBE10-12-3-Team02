package com.back.domain.concert.service

import com.back.domain.concert.repository.ConcertRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ConcertReviewSummaryCommandService(
    private val concertRepository: ConcertRepository,
) {
    // AFTER_COMMIT 시점엔 원본 트랜잭션이 이미 끝나 있어 REQUIRES_NEW/NOT_SUPPORTED 외의 propagation은
    // Spring의 RestrictedTransactionalEventListenerFactory(spring-tx, @since 6.1)가 컨텍스트 로딩 시점에 거부한다.
    // Concert 엔티티를 이벤트 발행 쪽에서 넘겨받지 않고 concertId만 받아 이 트랜잭션 안에서 직접 조회한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveSummary(concertId: Long, summary: String) {
        val concert = concertRepository.findById(concertId).orElse(null) ?: return
        concert.applyReviewSummary(summary)
    }
}
