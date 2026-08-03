package com.back.domain.concert.sse.scheduler

import com.back.domain.concert.sse.repository.SseOutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class SseOutboxEventCleaner(
    private val sseOutboxEventRepository: SseOutboxEventRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 * * * *") // 매시간 정각 실행
    @Transactional
    fun cleanOldOutboxEvents() {
        val limitDate = LocalDateTime.now().minusHours(1)
        val deletedCount = sseOutboxEventRepository.deleteByCreateDateBefore(limitDate)
        if (deletedCount > 0) {
            log.info("만료된 SSE Outbox 이벤트 정돈 완료: {}건 삭제 (기준: {})", deletedCount, limitDate)
        }
    }
}
