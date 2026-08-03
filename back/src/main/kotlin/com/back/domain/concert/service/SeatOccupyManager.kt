package com.back.domain.concert.service

import com.back.domain.concert.dto.SeatOccupyResponse
import com.back.domain.concert.dto.SeatSelectionResponse
import com.back.domain.concert.event.SeatOccupiedEvent
import com.back.domain.concert.event.SeatReleasedEvent
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.domain.ticket.dto.SeatHoldInfo
import com.back.domain.ticket.repository.TicketRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class SeatOccupyManager(
    private val concertService: ConcertService,
    private val ticketRepository: TicketRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val stringRedisTemplate: StringRedisTemplate,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun seatOccupy(concertId: Long, scheduleId: Long, seatNumber: String, userId: Long): SeatOccupyResponse {
        concertService.validateConcertScheduleMatch(concertId, scheduleId)
        concertService.validateScheduleBookable(scheduleId)

        val redisKey = generateSeatOccupyKey(concertId, scheduleId, seatNumber)
        val occupyToken = UUID.randomUUID().toString()

        val result: Long? = stringRedisTemplate.execute(
            OCCUPY_SCRIPT,
            listOf(redisKey),
            userId.toString(),
            occupyToken,
            OCCUPY_TTL_SECONDS.toString()
        )

        if (result == null || result == 0L) {
            throw ServiceException(ErrorCode.SEAT_HELD_BY_OTHER_USER)
        }

        try {
            val seat = scheduleSeatRepository
                .findBySchedule_ScheduleIdAndSeatNumber(scheduleId, seatNumber)
                ?: throw ServiceException(ErrorCode.SEAT_NOT_FOUND)

            seat.occupyHold()
        } catch (e: ServiceException) {
            if (e.errorCode != ErrorCode.SEAT_ALREADY_SOLD) {
                cleanupRedis(redisKey)
            }
            throw e
        } catch (e: Exception) {
            cleanupRedis(redisKey)
            throw e
        }

        eventPublisher.publishEvent(SeatOccupiedEvent(concertId, scheduleId, seatNumber, OCCUPY_TTL_SECONDS))

        return SeatOccupyResponse.of(occupyToken, OCCUPY_TTL_SECONDS)
    }

    @Transactional
    fun seatOccupyCancel(concertId: Long, scheduleId: Long, seatNumber: String, userId: Long) {
        concertService.validateConcertScheduleMatch(concertId, scheduleId)

        val redisKey = generateSeatOccupyKey(concertId, scheduleId, seatNumber)
        val entries = stringRedisTemplate.opsForHash<String, String>().entries(redisKey)

        val occupyUserId = entries["userId"]
        if (occupyUserId != null && occupyUserId != userId.toString()) {
            throw ServiceException(ErrorCode.SEAT_HELD_BY_OTHER_USER)
        }

        // Redis 정리는 AFTER_COMMIT 리스너에 위임
        // DB 커밋 이후에 Redis를 정리해야 DB 롤백 시 Redis 선점 키가 안전하게 보존됨
        val seat = scheduleSeatRepository.findBySchedule_ScheduleIdAndSeatNumber(scheduleId, seatNumber)
        seat?.releaseToAvailable()

        // 커밋 후 SSE 브로드캐스트 (SeatStatusSseBroadcaster.onSeatReleased)
        eventPublisher.publishEvent(SeatReleasedEvent(concertId, scheduleId, seatNumber))
    }

    @Transactional(readOnly = true)
    fun getSeatSelection(concertId: Long, scheduleId: Long, userId: Long): SeatSelectionResponse {
        concertService.validateConcertScheduleMatch(concertId, scheduleId)
        concertService.validateScheduleBookable(scheduleId)

        val currentTicketCount = ticketRepository
            .countByUser_UserIdAndSchedule_ScheduleIdAndIsValidTrue(userId, scheduleId)
        if (currentTicketCount >= 3) {
            throw ServiceException(ErrorCode.EXCEED_TICKET_LIMIT)
        }

        val seats = concertService.getScheduleSeats(scheduleId)
        val pricesMap = concertService.convertToPriceMap(seats)

        val seatResponses = seats.map { seat ->
            SeatSelectionResponse.SeatDetailResponse(
                seat.seatNumber,
                seat.seatStatus,
                seat.gradeName
            )
        }

        return SeatSelectionResponse.of(concertId, scheduleId, pricesMap, seatResponses)
    }

    fun cleanupRedis(redisKey: String) {
        stringRedisTemplate.delete(redisKey)
    }

    fun validateSeatHolds(userId: Long, concertId: Long, scheduleId: Long, seatHolds: List<SeatHoldInfo>) {
        for (hold in seatHolds) {
            val redisKey = generateSeatOccupyKey(concertId, scheduleId, hold.seatNumber)
            val entries = stringRedisTemplate.opsForHash<String, String>().entries(redisKey)

            val holdUserId = entries["userId"]
            val holdOccupyToken = entries["occupyToken"]

            if (holdUserId == null || holdOccupyToken == null) {
                throw ServiceException(ErrorCode.SEAT_HOLD_EXPIRED)
            }
            if (userId.toString() != holdUserId) {
                throw ServiceException(ErrorCode.SEAT_HELD_BY_OTHER_USER)
            }
            if (hold.occupyToken != holdOccupyToken) {
                throw ServiceException(ErrorCode.INVALID_OCCUPY_TOKEN)
            }
        }
    }

     // 좌석 선점 시 만료 ZSET에 등록함
     // Score = 만료 예정 Unix 타임스탬프, Member = concertId:scheduleId:seatNumber
    fun addToExpireQueue(concertId: Long, scheduleId: Long, seatNumber: String, ttlSeconds: Long) {
        val expireAt = System.currentTimeMillis() + ttlSeconds * 1000
        val member = buildExpireMember(concertId, scheduleId, seatNumber)
        stringRedisTemplate.opsForZSet().add(EXPIRE_QUEUE_KEY, member, expireAt.toDouble())
        log.debug("만료 큐 등록: {}, expireAt={}", member, expireAt)
    }

     // 결제 완료 또는 수동 취소 시 만료 ZSET에서 해당 좌석 메시지를 제거함
    fun removeFromExpireQueue(concertId: Long, scheduleId: Long, seatNumber: String) {
        try {
            val member = buildExpireMember(concertId, scheduleId, seatNumber)
            stringRedisTemplate.opsForZSet().remove(EXPIRE_QUEUE_KEY, member)
            log.debug("만료 큐 제거: {}", member)
        } catch (e: Exception) {
            log.warn("만료 큐 제거 실패 (무시됨): {}", e.message)
        }
    }

    companion object {
        const val OCCUPY_TTL_SECONDS: Long = 600

        private val OCCUPY_LUA = """
            if redis.call('EXISTS', KEYS[1]) == 1 then
              if redis.call('HGET', KEYS[1], 'userId') == ARGV[1] then
                redis.call('HSET', KEYS[1], 'occupyToken', ARGV[2])
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
                return 1
              else
                return 0
              end
            else
              redis.call('HSET', KEYS[1], 'userId', ARGV[1], 'occupyToken', ARGV[2])
              redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
              return 1
            end
        """.trimIndent()

        private val OCCUPY_SCRIPT: RedisScript<Long> = DefaultRedisScript(OCCUPY_LUA, Long::class.java)

        const val EXPIRE_QUEUE_KEY = "seat:hold:expire:queue"

        @JvmStatic
        fun generateSeatOccupyKey(concertId: Long, scheduleId: Long, seatNumber: String): String =
            "seat:occupy:$concertId:$scheduleId:$seatNumber"

        @JvmStatic
        fun buildExpireMember(concertId: Long, scheduleId: Long, seatNumber: String): String =
            "$concertId:$scheduleId:$seatNumber"
    }
}
