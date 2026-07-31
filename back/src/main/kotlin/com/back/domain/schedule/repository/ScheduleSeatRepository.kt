package com.back.domain.schedule.repository

import com.back.domain.schedule.entity.ScheduleSeat
import com.back.domain.schedule.constant.SeatStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import jakarta.persistence.QueryHint

interface ScheduleSeatRepository : JpaRepository<ScheduleSeat, Long> {

    fun findByScheduleScheduleId(scheduleId: Long): List<ScheduleSeat>

    fun findBySchedule_ScheduleIdAndSeatNumber(scheduleId: Long, seatNumber: String): ScheduleSeat?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ss
        from ScheduleSeat ss
        where ss.schedule.scheduleId = :scheduleId
          and ss.seatNumber = :seatNumber
    """)
    fun findWithLockByScheduleIdAndSeatNumber(
        @Param("scheduleId") scheduleId: Long,
        @Param("seatNumber") seatNumber: String
    ): ScheduleSeat?

    /**
     * 만료 스케줄러 전용: 이미 다른 트랜잭션이 해당 좌석을 처리 중이면 대기하지 않고 즉시 null 반환(SKIP LOCKED).
     * 결제/선점 트랜잭션이 좌석을 처리 중일 때 만료 처리가 충돌하지 않도록 방어한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
        select ss
        from ScheduleSeat ss
        where ss.schedule.scheduleId = :scheduleId
          and ss.seatNumber = :seatNumber
    """)
    fun findWithSkipLockByScheduleIdAndSeatNumber(
        @Param("scheduleId") scheduleId: Long,
        @Param("seatNumber") seatNumber: String
    ): ScheduleSeat?

    fun countBySchedule_ScheduleIdAndSeatStatus(scheduleId: Long, seatStatus: SeatStatus): Long

    fun countBySchedule_ScheduleIdAndSeatStatusIn(scheduleId: Long, seatStatuses: Collection<SeatStatus>): Long
}
