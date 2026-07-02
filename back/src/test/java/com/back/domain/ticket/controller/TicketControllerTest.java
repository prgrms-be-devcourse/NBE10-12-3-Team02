package com.back.domain.ticket.controller;

import com.back.domain.concert.entity.Concert;
import com.back.domain.concert.repository.ConcertRepository;
import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.repository.ScheduleRepository;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.LoginType;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.domain.venue.entity.Venue;
import com.back.domain.venue.repository.VenueRepository;
import com.back.global.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.back.domain.schedule.entity.SeatStatus.AVAILABLE;
import static com.back.domain.schedule.entity.SeatStatus.SOLD_OUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketControllerTest {
    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final ConcertRepository concertRepository;
    private final VenueRepository venueRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final TicketRepository ticketRepository;

    private User user;
    private SecurityUser securityUser;
    private Concert concert;
    private Schedule schedule;
    private ScheduleSeat seat;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    TicketControllerTest(
            MockMvc mockMvc,
            UserRepository userRepository,
            ConcertRepository concertRepository,
            VenueRepository venueRepository,
            ScheduleRepository scheduleRepository,
            ScheduleSeatRepository scheduleSeatRepository,
            TicketRepository ticketRepository
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.concertRepository = concertRepository;
        this.venueRepository = venueRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleSeatRepository = scheduleSeatRepository;
        this.ticketRepository = ticketRepository;
    }

    @BeforeEach
    void setUp() {
        user = saveUser();
        securityUser = new SecurityUser(user.getUserId(), user.getName());
        concert = concertRepository.save(Concert.create(
                "싸이 콘서트",
                "설명",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                "poster.jpg"
        ));
        Venue venue = venueRepository.save(Venue.create("공연장", "서울", 15000L));
        schedule = scheduleRepository.save(Schedule.create(concert, venue, LocalDateTime.now().plusHours(12), 1));
        seat = scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-1", 150000, AVAILABLE));

        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        when(hashOperations.multiGet(any(), anyList()))
                .thenReturn(List.of(user.getUserId().toString(), "test-token"));

        when(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class)))
                .thenReturn(List.of(false));
    }

    @Test
    @DisplayName("티켓 생성 성공")
    void createTicket() throws Exception {
        String requestBody = """
                {
                  "concertId": %d,
                  "scheduleId": %d,
                  "seatNumber": "A-1",
                  "occupyToken": "test-token"
                }
                """.formatted(concert.getConcertId(), schedule.getScheduleId());

        mockMvc.perform(post("/api/v1/tickets/reserve")
                        .with(user(securityUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("결제 및 티켓 생성 성공"))
                .andExpect(jsonPath("$.data.ticketNumber").isString())
                .andExpect(jsonPath("$.data.urlPoster").value("poster.jpg"))
                .andExpect(jsonPath("$.data.concertName").value("싸이 콘서트"))
                .andExpect(jsonPath("$.data.seatNumber").value("A-1"))
                .andExpect(jsonPath("$.data.seatStatus").value("SOLD_OUT"))
                .andExpect(jsonPath("$.data.isValid").value(true));

        assertThat(seat.getSeatStatus()).isEqualTo(SOLD_OUT);
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("티켓 취소 성공")
    void cancelTicket() throws Exception {
        seat.updateSeatStatus(SOLD_OUT);
        Ticket ticket = ticketRepository.save(Ticket.create(user, schedule, seat, "ticket-number", seat.getSeatPrice()));

        mockMvc.perform(patch("/api/v1/tickets/cancel/{ticketId}", ticket.getTicketId())
                        .with(user(securityUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("티켓 취소 성공"));

        assertThat(ticket.isValid()).isFalse();
        assertThat(seat.getSeatStatus()).isEqualTo(AVAILABLE);
    }

    private User saveUser() {
        return userRepository.save(
                User.create(
                        "user1",
                        "user1@test.com",
                        "0000",
                        "테스트 유저",
                        LoginType.NORMAL
                )
        );
    }
}
