package com.back.domain.schedule.repository;

import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long> {
    List<ScheduleSeat> findByScheduleScheduleId(Long scheduleId);

    Optional<ScheduleSeat> findByScheduleScheduleIdAndSeatNumber(Long scheduleId, String seatNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ss
        from ScheduleSeat ss
        where ss.schedule.scheduleId = :scheduleId
          and ss.seatNumber = :seatNumber
    """)
    Optional<ScheduleSeat> findWithLockByScheduleIdAndSeatNumber(
            @Param("scheduleId") Long scheduleId,
            @Param("seatNumber") String seatNumber
    );
    long countBySchedule_ScheduleIdAndSeatStatus(Long scheduleId, SeatStatus seatStatus);
}
