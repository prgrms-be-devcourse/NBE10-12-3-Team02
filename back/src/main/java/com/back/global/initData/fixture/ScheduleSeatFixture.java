package com.back.global.initData.fixture;

import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
import com.back.global.initData.generator.SeatGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleSeatFixture {
    private final ScheduleSeatRepository scheduleSeatRepository;

    public void createSeats(List<Schedule> schedules) {
        List<ScheduleSeat> allSeats = new ArrayList<>();
        for (Schedule schedule : schedules) {
            allSeats.addAll(SeatGenerator.generate(schedule));
        }
        scheduleSeatRepository.saveAll(allSeats);
    }
}