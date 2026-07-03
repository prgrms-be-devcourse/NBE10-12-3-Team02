package com.back.domain.concert.service;

import com.back.domain.concert.entity.Concert;
import com.back.domain.concert.repository.ConcertDeatilRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
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
@org.springframework.context.annotation.Import(com.back.global.RedisTestConfig.class)
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
    @Autowired
    private ConcertDeatilRepository concertDeatilRepository;

    @MockitoSpyBean
    private StringRedisTemplate redisTemplate;

    private Concert concert;
    private Schedule schedule;
    private ScheduleSeat seat;

    @BeforeEach
    void setUp() {
        scheduleSeatRepository.deleteAll();
        scheduleRepository.deleteAll();
        concertDeatilRepository.deleteAll();
        concertRepository.deleteAll();
        venueRepository.deleteAll();

        concert = concertRepository.save(Concert.create("아이유 콘서트", "설명", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "poster.jpg"));
        Venue venue = venueRepository.save(Venue.create("올림픽체조경기장", "서울", 15000L));
        schedule = scheduleRepository.save(Schedule.create(concert, venue, LocalDateTime.now().plusHours(12), 1));

        for (int i = 1; i <= 100; i++) {
            ScheduleSeat createdSeat = scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-" + i, 150000, SeatStatus.AVAILABLE));
            if (i == 1) {
                this.seat = createdSeat;
            }
        }

        java.util.Set<String> keys = redisTemplate.keys("seat:occupy:" + concert.getConcertId() + ":" + schedule.getScheduleId() + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        doAnswer(invocation -> {
            Thread.sleep(500);
            return invocation.callRealMethod();
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
        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger connectionTimeoutCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

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
                        String errName = e.getClass().getSimpleName();
                        String errMsg = e.getMessage() != null ? e.getMessage() : "";
                        Throwable cause = e.getCause();
                        String causeName = cause != null ? cause.getClass().getSimpleName() : "";
                        String causeMsg = cause != null && cause.getMessage() != null ? cause.getMessage() : "";

                        boolean isConnectionTimeout = errName.contains("Connection") ||
                                                     errName.contains("Timeout") ||
                                                     errName.contains("CannotCreateTransaction") ||
                                                     causeName.contains("Connection") ||
                                                     causeName.contains("Timeout") ||
                                                     errMsg.contains("HikariPool") ||
                                                     causeMsg.contains("HikariPool");

                        if (isConnectionTimeout) {
                            connectionTimeoutCount.incrementAndGet();
                        }

                        System.err.println(" 예외 발생 원인: " + errName + " - " + errMsg);
                        if (cause != null) {
                            System.err.println("   └─ 상세 원인: " + causeName + " - " + causeMsg);
                        }
                        failCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            doneLatch.await();
        }

        long endTime = System.currentTimeMillis();
        System.out.println(">>> [성능 리포트] 총 소요 시간: " + (endTime - startTime) + " ms");
        System.out.println(">>> [성능 리포트] 커넥션 고갈 예외 수: " + connectionTimeoutCount.get() + " / " + threadCount);

        assertThat(connectionTimeoutCount.get())
                .as("커넥션 고갈 예외가 0개여야 합니다.")
                .isEqualTo(0);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        ScheduleSeat updatedSeat = scheduleSeatRepository.findById(seat.getConcertSeatPriceId()).orElseThrow();
        assertThat(updatedSeat.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("파이프라이닝 성능 측정 테스트")
    void pipeliningBenchmark() {
        int requestCount = 100;

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < requestCount; i++) {
            seatOccupyManager.getSeatSelection(concert.getConcertId(), schedule.getScheduleId());
        }
        long endTime = System.currentTimeMillis();

        System.out.println(">>> [파이프라이닝 성능 리포트] 300회 조회 총 소요 시간: " + (endTime - startTime) + " ms");
    }
}
