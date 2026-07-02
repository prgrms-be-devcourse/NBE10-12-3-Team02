package com.back.domain.concert.service;

import com.back.domain.concert.entity.Concert;
import com.back.domain.concert.repository.ConcertRepository;
import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.entity.SeatStatus;
import com.back.domain.schedule.repository.ScheduleRepository;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
import com.back.domain.venue.entity.Venue;
import com.back.domain.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;

@ActiveProfiles("test")
@SpringBootTest
class ConcertServiceTest {
    @Autowired
    private SeatOccupyManager seatOccupyManager;
    @Autowired
    private ConcertRepository concertRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private ScheduleSeatRepository scheduleSeatRepository;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private Concert concert;
    private Schedule schedule;
    private ScheduleSeat seat;

    private final ConcurrentHashMap<String, String> localStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        scheduleSeatRepository.deleteAll();
        scheduleRepository.deleteAll();
        concertRepository.deleteAll();
        venueRepository.deleteAll();
        localStore.clear();

        concert = concertRepository.save(Concert.create("아이유 콘서트", "설명", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "poster.jpg"));
        Venue venue = venueRepository.save(Venue.create("올림픽체조경기장", "서울", 15000L));
        schedule = scheduleRepository.save(Schedule.create(concert, venue, LocalDateTime.now().plusHours(12), 1));
        seat = scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-1", 150000, SeatStatus.AVAILABLE));

        doAnswer(invocation -> {
            List<String> keys = invocation.getArgument(1);
            String key = keys.get(0);
            String userId = invocation.getArgument(2).toString();

            if (localStore.putIfAbsent(key, userId) == null) {
                return 1L;
            }
            return 0L;
        }).when(redisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                any(Object.class),
                any(Object.class),
                any(Object.class)
        );
    }

    @Test
    @DisplayName("실시간 좌석 선점 동시성 테스트")
    void seatOccupy() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final long userId = i + 1;
                executorService.execute(() -> {
                    try {
                        startLatch.await();
                        seatOccupyManager.seatOccupy(
                                concert.getConcertId(),
                                schedule.getScheduleId(),
                                seat.getSeatNumber(),
                                userId
                        );
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            doneLatch.await();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        ScheduleSeat updatedSeat = scheduleSeatRepository.findById(seat.getConcertSeatPriceId()).orElseThrow();
        assertThat(updatedSeat.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
}