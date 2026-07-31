package com.back.domain.concert.service

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertDeatilRepository
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.schedule.constant.SeatStatus
import com.back.domain.schedule.entity.Schedule
import com.back.domain.schedule.entity.ScheduleSeat
import com.back.domain.schedule.repository.ScheduleRepository
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.domain.venue.entity.Venue
import com.back.domain.venue.repository.VenueRepository
import com.back.global.RedisTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisTestConfig::class)
class ConcertServiceTest {
    @Autowired
    private lateinit var seatOccupyManager: SeatOccupyManager

    @Autowired
    private lateinit var concertRepository: ConcertRepository

    @Autowired
    private lateinit var venueRepository: VenueRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var scheduleSeatRepository: ScheduleSeatRepository

    @Autowired
    private lateinit var concertDeatilRepository: ConcertDeatilRepository

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    private lateinit var concert: Concert
    private lateinit var schedule: Schedule
    private lateinit var seat: ScheduleSeat

    @BeforeEach
    fun setUp() {
        scheduleSeatRepository.deleteAll()
        scheduleRepository.deleteAll()
        concertDeatilRepository.deleteAll()
        concertRepository.deleteAll()
        venueRepository.deleteAll()

        concert = concertRepository.save(
            Concert.create("아이유 콘서트", "설명", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "poster.jpg")
        )
        val venue = venueRepository.save(Venue.create("올림픽체조경기장", "서울", 15000L))
        schedule = scheduleRepository.save(Schedule.create(concert, venue, LocalDateTime.now().plusHours(12), 1))

        for (i in 1..100) {
            val createdSeat = scheduleSeatRepository.save(
                ScheduleSeat.create(schedule, "VIP", "A-$i", 150000, SeatStatus.AVAILABLE)
            )
            if (i == 1) {
                this.seat = createdSeat
            }
        }

        val pattern = "seat:occupy:${concert.concertId}:${schedule.scheduleId}:*"
        val keys = stringRedisTemplate.keys(pattern)
        if (!keys.isNullOrEmpty()) {
            stringRedisTemplate.delete(keys)
        }
    }

    @Test
    @DisplayName("실시간 좌석 선점 동시성 테스트")
    fun seatOccupy() {
        val threadCount = 10
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        val executor = Executors.newFixedThreadPool(threadCount)

        for (i in 1..threadCount) {
            val userId = i.toLong()
            executor.submit {
                try {
                    startLatch.await()
                    seatOccupyManager.seatOccupy(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber, userId)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(threadCount - 1)

        val updatedSeat = scheduleSeatRepository.findById(seat.concertSeatPriceId!!).get()
        assertThat(updatedSeat.seatStatus).isEqualTo(SeatStatus.HOLD)
    }

    @Test
    @DisplayName("다양한 좌석에 대한 동시성 테스트")
    fun seatOccupy2() {
        val threadCount = 100
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        val executor = Executors.newFixedThreadPool(threadCount)

        for (i in 1..threadCount) {
            val userId = i.toLong()
            val seatNumber = "A-$i"
            executor.submit {
                try {
                    startLatch.await()
                    seatOccupyManager.seatOccupy(concert.concertId!!, schedule.scheduleId!!, seatNumber, userId)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(successCount.get()).isEqualTo(100)
        assertThat(failCount.get()).isEqualTo(0)
    }
}
