package com.back.domain.waiting.service

import com.back.domain.schedule.repository.ScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WaitingQueueScheduler(
    private val waitingQueueService: WaitingQueueService,
    private val waitingQueueManager: WaitingQueueManager,
    private val scheduleRepository: ScheduleRepository,
) {

    @Scheduled(fixedDelay = 1000)
    fun processExpiredActiveUsers() {
        val activeScheduleIds = waitingQueueManager.getActiveScheduleIds()
        if (activeScheduleIds.isEmpty()) return

        for (scheduleIdValue in activeScheduleIds) {
            try {
                processSchedule(scheduleIdValue)
            } catch (e: Exception) {
                log.error(
                    "대기열 만료 사용자 처리 실패: scheduleId={}, error={}",
                    scheduleIdValue,
                    e.message,
                    e,
                )
            }
        }
    }

    private fun processSchedule(scheduleIdValue: String) {
        val scheduleId = scheduleIdValue.toLongOrNull()
        if (scheduleId == null) {
            log.warn("잘못된 대기열 회차 ID를 활성 회차 목록에서 제거: scheduleId={}", scheduleIdValue)
            waitingQueueManager.removeFromActiveSchedules(scheduleIdValue)
            return
        }

        // 대기자가 없다면 회차 및 좌석 DB 조회를 생략하고 Redis 만료 상태만 정리한다.
        // 확인 직후 사용자가 등록되더라도 등록 API가 allowEntry()를 호출하므로 입장은 다시 조정된다.
        if (!waitingQueueManager.hasWaitingUsers(scheduleId)) {
            waitingQueueManager.removeExpiredActiveUsers(scheduleId)
            removeInactiveScheduleIfEmpty(scheduleId, scheduleIdValue)
            return
        }

        // Redis ACTIVE 상태를 변경하기 전에 회차 정보를 확보한다. DB 조회가 실패하면
        // Redis 상태를 그대로 두어 다음 스케줄러 실행에서 다시 처리할 수 있게 한다.
        val schedule = scheduleRepository.findByScheduleId(scheduleId)
        if (schedule == null) {
            log.warn("대기열 회차 정보를 찾을 수 없음: scheduleId={}", scheduleId)
            return
        }
        val concertId = schedule.concert.concertId
        if (concertId == null) {
            log.warn("대기열 회차의 콘서트 ID를 찾을 수 없음: scheduleId={}", scheduleId)
            return
        }

        waitingQueueManager.removeExpiredActiveUsers(scheduleId)

        // 직전 실행에서 만료 사용자를 제거한 뒤 입장 보충에 실패했더라도,
        // expired 반환값과 무관하게 매 주기 재조정하여 다음 실행에서 복구한다.
        waitingQueueService.allowEntry(concertId, scheduleId)

        removeInactiveScheduleIfEmpty(scheduleId, scheduleIdValue)
    }

    private fun removeInactiveScheduleIfEmpty(scheduleId: Long, scheduleIdValue: String) {
        if (waitingQueueManager.isQueueEmpty(scheduleId)) {
            waitingQueueManager.removeFromActiveSchedules(scheduleIdValue)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(WaitingQueueScheduler::class.java)
    }
}
