package com.back.domain.concert.service

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.schedule.constant.SeatStatus
import com.back.domain.schedule.entity.Schedule
import com.back.domain.schedule.entity.ScheduleSeat
import com.back.domain.schedule.repository.ScheduleRepository
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.domain.venue.entity.Venue
import com.back.domain.venue.repository.VenueRepository
import com.back.global.RedisTestConfig
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisTestConfig::class)
class SeatOccupyManagerTest {

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
    private lateinit var stringRedisTemplate: StringRedisTemplate

    private lateinit var concert: Concert
    private lateinit var schedule: Schedule
    private lateinit var seat: ScheduleSeat

    @BeforeEach
    fun setUp() {
        scheduleSeatRepository.deleteAll()
        scheduleRepository.deleteAll()
        concertRepository.deleteAll()
        venueRepository.deleteAll()

        concert = concertRepository.save(
            Concert.create("아이유 콘서트", "설명", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "poster.jpg")
        )
        val venue = venueRepository.save(Venue.create("체조경기장", "서울", 10000L))
        schedule = scheduleRepository.save(Schedule.create(concert, venue, LocalDateTime.now().plusHours(12), 1))
        seat = scheduleSeatRepository.save(
            ScheduleSeat.create(schedule, "VIP", "A-1", 150000, SeatStatus.AVAILABLE)
        )

        val pattern = "seat:occupy:${concert.concertId}:${schedule.scheduleId}:*"
        val keys = stringRedisTemplate.keys(pattern)
        if (!keys.isNullOrEmpty()) {
            stringRedisTemplate.delete(keys)
        }
        stringRedisTemplate.delete(SeatOccupyManager.EXPIRE_QUEUE_KEY)
    }

    @Test
    @DisplayName("좌석 선점 성공 시 Redis에 userId와 occupyToken이 저장되고 DB 상태가 HOLD로 변경된다")
    fun seatOccupy_success() {
        val userId = 100L
        val response = seatOccupyManager.seatOccupy(
            concert.concertId!!,
            schedule.scheduleId!!,
            seat.seatNumber,
            userId
        )

        assertThat(response.occupyToken).isNotNull()
        assertThat(response.expireInSeconds).isEqualTo(SeatOccupyManager.OCCUPY_TTL_SECONDS)

        val redisKey = SeatOccupyManager.generateSeatOccupyKey(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber)
        val entries = stringRedisTemplate.opsForHash<String, String>().entries(redisKey)

        assertThat(entries["userId"]).isEqualTo(userId.toString())
        assertThat(entries["occupyToken"]).isEqualTo(response.occupyToken)

        val updatedSeat = scheduleSeatRepository.findById(seat.concertSeatPriceId!!).get()
        assertThat(updatedSeat.seatStatus).isEqualTo(SeatStatus.HOLD)
    }

    @Test
    @DisplayName("이미 선점된 좌석을 다른 사용자가 선점 시도 시 SEAT_HELD_BY_OTHER_USER 예외가 발생한다")
    fun seatOccupy_duplicate_throwsException() {
        val user1 = 100L
        val user2 = 200L

        seatOccupyManager.seatOccupy(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber, user1)

        assertThatThrownBy {
            seatOccupyManager.seatOccupy(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber, user2)
        }.isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.SEAT_HELD_BY_OTHER_USER.message)
    }

    @Test
    @DisplayName("좌석 선점 취소 시 Redis 선점 정보와 만료 ZSET이 함께 제거되고 DB 상태가 AVAILABLE로 복구된다")
    fun seatOccupyCancel_success() {
        val userId = 100L
        seatOccupyManager.seatOccupy(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber, userId)
        seatOccupyManager.addToExpireQueue(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber, 600)

        seatOccupyManager.seatOccupyCancel(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber, userId)

        val redisKey = SeatOccupyManager.generateSeatOccupyKey(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber)
        val exists = stringRedisTemplate.hasKey(redisKey)
        assertThat(exists).isFalse()

        val member = SeatOccupyManager.buildExpireMember(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber)
        val score = stringRedisTemplate.opsForZSet().score(SeatOccupyManager.EXPIRE_QUEUE_KEY, member)
        assertThat(score as Any?).isNull()

        val updatedSeat = scheduleSeatRepository.findById(seat.concertSeatPriceId!!).get()
        assertThat(updatedSeat.seatStatus).isEqualTo(SeatStatus.AVAILABLE)
    }

    @Test
    @DisplayName("ZSET 만료 큐 등록 및 제거 기능 테스트")
    fun expireQueue_addAndRemove() {
        val member = SeatOccupyManager.buildExpireMember(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber)

        seatOccupyManager.addToExpireQueue(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber, 300)

        val score = stringRedisTemplate.opsForZSet().score(SeatOccupyManager.EXPIRE_QUEUE_KEY, member)
        assertThat(score as Any?).isNotNull
        assertThat(score!!).isGreaterThan(System.currentTimeMillis().toDouble())

        seatOccupyManager.removeFromExpireQueue(concert.concertId!!, schedule.scheduleId!!, seat.seatNumber)

        val removedScore = stringRedisTemplate.opsForZSet().score(SeatOccupyManager.EXPIRE_QUEUE_KEY, member)
        assertThat(removedScore as Any?).isNull()
    }
}
