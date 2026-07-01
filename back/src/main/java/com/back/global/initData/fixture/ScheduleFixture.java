package com.back.global.initData.fixture;

import com.back.domain.concert.entity.Concert;
import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.repository.ScheduleRepository;
import com.back.domain.venue.entity.Venue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleFixture {
    private final ScheduleRepository scheduleRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime dt(String value) {
        return LocalDateTime.parse(value, FMT);
    }

    public List<Schedule> createSchedules(List<Concert> concerts, List<Venue> venues) {
        List<Schedule> s = new ArrayList<>();

        s.add(Schedule.create(concerts.get(0), venues.get(0), dt("2026-06-20 14:00:00"), 1));
        s.add(Schedule.create(concerts.get(0), venues.get(1), dt("2026-07-12 18:00:00"), 2));
        s.add(Schedule.create(concerts.get(1), venues.get(21), dt("2026-06-30 15:00:00"), 1));
        s.add(Schedule.create(concerts.get(1), venues.get(4), dt("2026-07-15 19:00:00"), 2));
        s.add(Schedule.create(concerts.get(1), venues.get(1), dt("2026-07-28 20:00:00"), 3));
        s.add(Schedule.create(concerts.get(2), venues.get(1), dt("2026-06-29 14:00:00"), 1));
        s.add(Schedule.create(concerts.get(2), venues.get(2), dt("2026-07-10 18:00:00"), 2));
        s.add(Schedule.create(concerts.get(3), venues.get(3), dt("2026-06-15 19:00:00"), 1));
        s.add(Schedule.create(concerts.get(3), venues.get(3), dt("2026-08-10 19:00:00"), 2));
        s.add(Schedule.create(concerts.get(4), venues.get(4), dt("2026-06-20 16:00:00"), 1));
        s.add(Schedule.create(concerts.get(5), venues.get(5), dt("2026-06-22 17:00:00"), 1));
        s.add(Schedule.create(concerts.get(5), venues.get(5), dt("2026-08-30 19:00:00"), 2));
        s.add(Schedule.create(concerts.get(6), venues.get(6), dt("2026-07-05 14:00:00"), 1));
        s.add(Schedule.create(concerts.get(6), venues.get(6), dt("2026-07-20 16:00:00"), 2));
        s.add(Schedule.create(concerts.get(7), venues.get(7), dt("2026-06-25 18:00:00"), 1));
        s.add(Schedule.create(concerts.get(8), venues.get(2), dt("2026-07-10 15:00:00"), 1));
        s.add(Schedule.create(concerts.get(8), venues.get(8), dt("2026-10-12 18:00:00"), 2));
        s.add(Schedule.create(concerts.get(8), venues.get(10), dt("2026-12-01 19:00:00"), 3));
        s.add(Schedule.create(concerts.get(9), venues.get(11), dt("2026-07-06 13:00:00"), 1));
        s.add(Schedule.create(concerts.get(9), venues.get(9), dt("2026-10-20 15:00:00"), 2));
        s.add(Schedule.create(concerts.get(9), venues.get(8), dt("2027-02-14 17:00:00"), 3));
        s.add(Schedule.create(concerts.get(10), venues.get(10), dt("2026-05-10 14:00:00"), 1));
        s.add(Schedule.create(concerts.get(10), venues.get(9), dt("2026-06-21 16:00:00"), 2));
        s.add(Schedule.create(concerts.get(11), venues.get(11), dt("2026-06-15 18:00:00"), 1));
        s.add(Schedule.create(concerts.get(11), venues.get(11), dt("2026-07-20 20:00:00"), 2));
        s.add(Schedule.create(concerts.get(12), venues.get(9), dt("2026-07-05 14:00:00"), 1));
        s.add(Schedule.create(concerts.get(12), venues.get(12), dt("2026-09-23 18:00:00"), 2));
        s.add(Schedule.create(concerts.get(13), venues.get(13), dt("2026-03-14 19:00:00"), 1));
        s.add(Schedule.create(concerts.get(13), venues.get(13), dt("2026-12-24 20:00:00"), 2));
        s.add(Schedule.create(concerts.get(13), venues.get(13), dt("2027-02-14 19:00:00"), 3));
        s.add(Schedule.create(concerts.get(14), venues.get(14), dt("2026-04-12 13:00:00"), 1));
        s.add(Schedule.create(concerts.get(14), venues.get(15), dt("2026-09-20 15:00:00"), 2));
        s.add(Schedule.create(concerts.get(15), venues.get(15), dt("2026-06-01 18:00:00"), 1));
        s.add(Schedule.create(concerts.get(15), venues.get(15), dt("2026-08-15 19:00:00"), 2));
        s.add(Schedule.create(concerts.get(16), venues.get(16), dt("2026-06-01 16:00:00"), 1));
        s.add(Schedule.create(concerts.get(16), venues.get(16), dt("2026-07-01 19:00:00"), 2));
        s.add(Schedule.create(concerts.get(17), venues.get(18), dt("2026-07-05 15:00:00"), 1));
        s.add(Schedule.create(concerts.get(17), venues.get(17), dt("2026-08-20 18:00:00"), 2));
        s.add(Schedule.create(concerts.get(18), venues.get(18), dt("2026-07-13 19:00:00"), 1));
        s.add(Schedule.create(concerts.get(19), venues.get(20), dt("2026-06-20 19:00:00"), 1));
        s.add(Schedule.create(concerts.get(19), venues.get(19), dt("2026-07-13 20:00:00"), 2));
        s.add(Schedule.create(concerts.get(20), venues.get(20), dt("2026-07-05 18:00:00"), 1));
        s.add(Schedule.create(concerts.get(20), venues.get(20), dt("2026-08-23 19:00:00"), 2));
        s.add(Schedule.create(concerts.get(21), venues.get(21), dt("2026-08-02 19:00:00"), 1));
        s.add(Schedule.create(concerts.get(22), venues.get(22), dt("2026-07-12 14:00:00"), 1));
        s.add(Schedule.create(concerts.get(22), venues.get(22), dt("2026-08-22 16:00:00"), 2));
        s.add(Schedule.create(concerts.get(23), venues.get(23), dt("2026-07-01 18:00:00"), 1));
        s.add(Schedule.create(concerts.get(23), venues.get(23), dt("2026-07-19 19:00:00"), 2));
        s.add(Schedule.create(concerts.get(24), venues.get(24), dt("2026-07-10 14:00:00"), 1));
        s.add(Schedule.create(concerts.get(24), venues.get(24), dt("2026-07-26 16:00:00"), 2));
        s.add(Schedule.create(concerts.get(25), venues.get(25), dt("2026-07-31 15:00:00"), 1));
        s.add(Schedule.create(concerts.get(26), venues.get(25), dt("2026-07-16 19:00:00"), 1));
        s.add(Schedule.create(concerts.get(27), venues.get(26), dt("2026-07-20 18:00:00"), 1));
        s.add(Schedule.create(concerts.get(27), venues.get(27), dt("2026-08-29 19:00:00"), 2));
        s.add(Schedule.create(concerts.get(28), venues.get(24), dt("2026-07-12 18:00:00"), 1));
        s.add(Schedule.create(concerts.get(28), venues.get(28), dt("2026-08-15 19:00:00"), 2));
        s.add(Schedule.create(concerts.get(28), venues.get(26), dt("2026-09-12 20:00:00"), 3));

        return scheduleRepository.saveAll(s);
    }
}
