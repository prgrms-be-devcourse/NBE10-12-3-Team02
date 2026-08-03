package com.back.domain.schedule.service

import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.concert.sse.SeatStatusETagVersionManager
import com.back.domain.schedule.constant.SeatStatus
import com.back.domain.schedule.dto.ShowScheduleListResponse
import com.back.domain.schedule.dto.ShowScheduleResponse
import com.back.domain.schedule.repository.ScheduleRepository
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val concertRepository: ConcertRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val eTagVersionManager: SeatStatusETagVersionManager
) {
    fun showSchedule(concertId: Long, scheduleId: Long): ShowScheduleResponse {
        val schedule = scheduleRepository
            .findByScheduleIdAndConcert_ConcertId(scheduleId, concertId)
            ?: throw ServiceException(ErrorCode.CONCERT_NOT_FOUND_OR_MISMATCH)

        val remainingSeats = scheduleSeatRepository
            .countBySchedule_ScheduleIdAndSeatStatus(scheduleId, SeatStatus.AVAILABLE)

        return ShowScheduleResponse.of(schedule, remainingSeats)
    }

     // DB 전체 좌석 조회 대신 Redis 버전 카운터를 사용하여 DB 부하를 제거
     // 좌석 상태 변경 이벤트 발생 시 SeatStatusSseBroadcaster가 Redis 버전을 증가
    fun getSeatStatusFingerprint(scheduleId: Long): String =
        eTagVersionManager.getVersion(scheduleId).toString()

    fun showScheduleList(concertId: Long): List<ShowScheduleListResponse> {
        if (!concertRepository.existsById(concertId)) {
            throw ServiceException(ErrorCode.CONCERT_NOT_FOUND)
        }

        val schedules = scheduleRepository.findByConcertConcertId(concertId)

        if (schedules.isEmpty()) {
            throw ServiceException(ErrorCode.CONCERT_SCHEDULE_EMPTY)
        }

        return schedules.map { schedule ->
            val scheduleId = checkNotNull(schedule.scheduleId) { "Schedule ID must not be null" }
            val remainingSeats = scheduleSeatRepository
                .countBySchedule_ScheduleIdAndSeatStatus(scheduleId, SeatStatus.AVAILABLE)
            ShowScheduleListResponse.of(schedule, remainingSeats)
        }
    }
}
