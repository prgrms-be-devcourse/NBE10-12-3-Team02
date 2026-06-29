package com.back.domain.schedule.repository;

import com.back.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    Optional<Schedule> findByScheduleIdAndConcert_ConcertId(Long scheduleId, Long concertId);
}
