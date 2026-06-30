package com.back.domain.concert.controller;

import com.back.domain.concert.entity.Concert;
import com.back.domain.concert.repository.ConcertRepository;
import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.repository.ScheduleRepository;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
import com.back.domain.venue.entity.Venue;
import com.back.domain.venue.repository.VenueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.back.domain.schedule.entity.SeatStatus.AVAILABLE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConcertControllerTest {
    private final MockMvc mockMvc;
    private final ConcertRepository concertRepository;
    private final VenueRepository venueRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    @Autowired
    public ConcertControllerTest(
            MockMvc mockMvc,
            ConcertRepository concertRepository,
            VenueRepository venueRepository,
            ScheduleRepository scheduleRepository,
            ScheduleSeatRepository scheduleSeatRepository
    ) {
        this.mockMvc = mockMvc;
        this.concertRepository = concertRepository;
        this.venueRepository = venueRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleSeatRepository = scheduleSeatRepository;
    }

    @Test
    @DisplayName("좌석 선택 페이지 조회 성공")
    void t1() throws Exception {
        Concert concert = Concert.create("아이유 콘서트", "설명", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "포스터");
        concertRepository.save(concert);

        Venue venue = Venue.create("올림픽체조경기장", "서울", 15000L);
        venueRepository.save(venue);

        Schedule schedule = Schedule.create(concert, venue, LocalDateTime.now().plusHours(12), 1);
        scheduleRepository.save(schedule);

        ScheduleSeat seat1 = ScheduleSeat.create(
                schedule,
                "VIP",
                "A-1",
                150000,
                AVAILABLE
        );
        scheduleSeatRepository.save(seat1);

        ScheduleSeat seat2 = ScheduleSeat.create(
                schedule,
                "A",
                "B-2",
                70000,
                AVAILABLE
        );
        scheduleSeatRepository.save(seat2);

        mockMvc.perform(get("/api/v1/concerts/{concertId}/schedules/{scheduleId}/seats", concert.getConcertId(), schedule.getScheduleId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("좌석 선택 페이지 조회 성공"))
                .andExpect(jsonPath("$.data.concertId").value(concert.getConcertId()))
                .andExpect(jsonPath("$.data.scheduleId").value(schedule.getScheduleId()))
                .andExpect(jsonPath("$.data.prices.VIP").value(150000))
                .andExpect(jsonPath("$.data.prices.A").value(70000))
                .andExpect(jsonPath("$.data.seats[0].seatNumber").value("A-1"))
                .andExpect(jsonPath("$.data.seats[0].seatStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.seats[0].gradeName").value("VIP"))
                .andExpect(jsonPath("$.data.seats[1].seatNumber").value("B-2"))
                .andExpect(jsonPath("$.data.seats[1].seatStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.seats[1].gradeName").value("A"));
    }
}
