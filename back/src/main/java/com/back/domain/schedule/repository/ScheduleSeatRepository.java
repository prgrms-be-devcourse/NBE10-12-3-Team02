package com.back.domain.schedule.repository;

import com.back.domain.schedule.entity.ScheduleSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long> {
    List<ScheduleSeat> findByScheduleScheduleId(Long scheduleId);
}
