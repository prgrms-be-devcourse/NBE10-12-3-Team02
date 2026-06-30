package com.back.domain.schedule.service;

import com.back.domain.concert.repository.ConcertRepository;
import com.back.domain.schedule.dto.ShowScheduleResponse;
import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.SeatStatus;
import com.back.domain.schedule.repository.ScheduleRepository;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ConcertRepository concertRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    public ShowScheduleResponse showSchedule(Long concertId,Long scheduleId) {

        Schedule schedule = scheduleRepository
                .findByScheduleIdAndConcert_ConcertId(scheduleId, concertId)
                .orElseThrow(() -> new ServiceException(ErrorCode.CONCERT_NOT_FOUND_OR_MISMATCH));

        long remainingSeats = scheduleSeatRepository
                .countBySchedule_ScheduleIdAndSeatStatus(scheduleId, SeatStatus.AVAILABLE);

        return ShowScheduleResponse.from(schedule, remainingSeats);
    }

}
