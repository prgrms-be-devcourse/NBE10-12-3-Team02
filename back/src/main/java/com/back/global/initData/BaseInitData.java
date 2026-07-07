package com.back.global.initData;

import com.back.domain.concert.entity.Concert;
import com.back.domain.concert.repository.ConcertRepository;
import com.back.domain.schedule.entity.Schedule;
import com.back.domain.venue.entity.Venue;
import com.back.global.initData.fixture.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class BaseInitData implements CommandLineRunner {

    private final ConcertRepository concertRepository;
    private final VenueFixture venueFixture;
    private final ConcertFixture concertFixture;
    private final ConcertDetailFixture concertDetailFixture;
    private final ScheduleFixture scheduleFixture;
    private final ScheduleSeatFixture scheduleSeatFixture;

    @Override
    @Transactional
    public void run(String... args) {
        if (concertRepository.count() > 0) {
            return;
        }

        List<Venue> venues = venueFixture.createVenues();
        List<Concert> concerts = concertFixture.createConcerts();
        concertDetailFixture.createDetails(concerts);
        List<Schedule> schedules = scheduleFixture.createSchedules(concerts, venues);
        scheduleSeatFixture.createSeats(schedules);
    }
}