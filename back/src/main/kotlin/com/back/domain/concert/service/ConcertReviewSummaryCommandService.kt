package com.back.domain.concert.service

import com.back.domain.concert.repository.ConcertRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ConcertReviewSummaryCommandService(
    private val concertRepository: ConcertRepository,
) {
    // AFTER_COMMIT 리스너에서 호출되므로 REQUIRES_NEW/NOT_SUPPORTED 외의 propagation은 Spring이 컨텍스트 로딩 시점에 거부한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveSummary(concertId: Long, summary: String) {
        val concert = concertRepository.findById(concertId).orElse(null) ?: return
        concert.applyReviewSummary(summary)
    }
}
