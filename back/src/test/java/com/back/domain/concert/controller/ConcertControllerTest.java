package com.back.domain.concert.controller;

import com.back.domain.concert.entity.Concert;
import com.back.domain.concert.repository.ConcertRepository;
import com.back.domain.concert.service.SeatOccupyManager;
import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.repository.ScheduleRepository;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.back.domain.schedule.entity.SeatStatus.AVAILABLE;
import static org.mockito.ArgumentMatchers.*;
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
class ConcertControllerTest {
    private final MockMvc mockMvc;
    private final ConcertRepository concertRepository;
    private final VenueRepository venueRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    private Concert concert;
    private Schedule schedule;

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

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("test-queue-token");

        concert = Concert.create("아이유 콘서트", "설명", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "poster.jpg");
        concertRepository.save(concert);

        Venue venue = Venue.create("올림픽체조경기장", "서울", 15000L);
        venueRepository.save(venue);

        schedule = Schedule.create(concert, venue, LocalDateTime.now().plusHours(12), 1);
        scheduleRepository.save(schedule);
    }

    @Test
    @DisplayName("좌석 선택 페이지 조회 성공")
    void t1() throws Exception {
        ScheduleSeat seat1 = ScheduleSeat.create(schedule, "VIP", "A-1", 150000, AVAILABLE);
        scheduleSeatRepository.save(seat1);

        ScheduleSeat seat2 = ScheduleSeat.create(schedule, "A", "B-2", 70000, AVAILABLE);
        scheduleSeatRepository.save(seat2);

        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        when(zSetOperations.score(anyString(), anyString()))
                .thenReturn((double) (System.currentTimeMillis() + 600000));

        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble()))
                .thenReturn(java.util.Collections.emptySet());

        mockMvc.perform(get("/api/v1/concerts/{concertId}/schedules/{scheduleId}/seats", concert.getConcertId(), schedule.getScheduleId())
                        .header("X-Queue-Token", "test-queue-token")
                        .with(user(new SecurityUser(1L, "테스트유저")))
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

    @Test
    @DisplayName("콘서트 목록 조회 성공")
    void t2() throws Exception {
        mockMvc.perform(get("/api/v1/concerts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("콘서트 목록 조회 성공"))
                .andExpect(jsonPath("$.data[0].concertName").value("아이유 콘서트"))
                .andExpect(jsonPath("$.data[0].venueName").value("올림픽체조경기장"))
                .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("콘서트 상세 조회 성공")
    void t3() throws Exception {
        ScheduleSeat seat = ScheduleSeat.create(schedule, "VIP", "A-1", 150000, AVAILABLE);
        scheduleSeatRepository.save(seat);

        mockMvc.perform(get("/api/v1/concerts/{concertId}", concert.getConcertId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("콘서트 상세 정보 조회 성공"))
                .andExpect(jsonPath("$.data.concertId").value(concert.getConcertId()))
                .andExpect(jsonPath("$.data.concertName").value("아이유 콘서트"))
                .andExpect(jsonPath("$.data.description").value("설명"))
                .andExpect(jsonPath("$.data.venueName").value("올림픽체조경기장"))
                .andExpect(jsonPath("$.data.location").value("서울"))
                .andExpect(jsonPath("$.data.prices.VIP").value(150000))
                .andExpect(jsonPath("$.data.bookable").value(true));
    }

    @Test
    @DisplayName("좌석 임시 선점 성공")
    void t4() throws Exception {
        ScheduleSeat seat = ScheduleSeat.create(schedule, "VIP", "A-1", 150000, AVAILABLE);
        scheduleSeatRepository.save(seat);

        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(1L);

        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        when(zSetOperations.score(anyString(), anyString()))
                .thenReturn((double) (System.currentTimeMillis() + 600000));

        String requestBody = """
                {
                  "seatNumber": "A-1"
                }
                """;

        mockMvc.perform(post("/api/v1/concerts/{concertId}/schedules/{scheduleId}/seats/occupy", concert.getConcertId(), schedule.getScheduleId())
                        .header("X-Queue-Token", "test-queue-token")
                        .with(user(new SecurityUser(1L, "테스트유저")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("좌석 임시 선점에 성공했습니다."))
                .andExpect(jsonPath("$.data.occupyToken").isString())
                .andExpect(jsonPath("$.data.expireInSeconds").value(600))
                .andExpect(jsonPath("$.data.seatStatus").value("HOLD"));
    }

    @Test
    @DisplayName("좌석 임시 선점 취소 성공")
    void t5() throws Exception {
        Long userId = 1L;
        String seatNumber = "A-1";
        String redisKey = SeatOccupyManager.generateSeatOccupyKey(concert.getConcertId(), schedule.getScheduleId(), seatNumber);

        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(redisKey, "userId")).thenReturn(userId.toString());

        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        String requestBody = """
                {
                  "seatNumber": "A-1"
                }
                """;

        mockMvc.perform(delete("/api/v1/concerts/{concertId}/schedules/{scheduleId}/seats/occupy", concert.getConcertId(), schedule.getScheduleId())
                        .with(user(new SecurityUser(userId, "테스트유저")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("좌석 선점이 정상적으로 취소되었습니다."));
    }
}
