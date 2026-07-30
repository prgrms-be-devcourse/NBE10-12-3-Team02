package com.back.domain.concert.service

import com.back.domain.concert.dto.SeatOccupyResponse
import com.back.domain.concert.dto.SeatSelectionResponse
import com.back.domain.concert.event.SeatOccupiedEvent
import com.back.domain.concert.event.SeatReleasedEvent
import com.back.domain.concert.listener.SeatHoldExpiredHandler
import com.back.domain.concert.listener.SeatOccupiedEventListener
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.domain.ticket.dto.SeatHoldInfo
import com.back.domain.ticket.repository.TicketRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class SeatOccupyManager(
    private val concertService: ConcertService,
    private val ticketRepository: TicketRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val redissonClient: RedissonClient,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun seatOccupy(concertId: Long, scheduleId: Long, seatNumber: String, userId: Long): SeatOccupyResponse {
        concertService.validateConcertScheduleMatch(concertId, scheduleId)
        concertService.validateScheduleBookable(scheduleId)

        val redisKey = generateSeatOccupyKey(concertId, scheduleId, seatNumber)
        val occupyToken = UUID.randomUUID().toString()

        val result: Long? = redissonClient.getScript(StringCodec.INSTANCE).eval(
            RScript.Mode.READ_WRITE,
            OCCUPY_SCRIPT,
            RScript.ReturnType.LONG,
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
                .findWithLockByScheduleIdAndSeatNumber(scheduleId, seatNumber)
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
        val hashMap = redissonClient.getMap<String, String>(redisKey, StringCodec.INSTANCE)

        val occupyUserId = hashMap["userId"]
        if (occupyUserId != null && occupyUserId != userId.toString()) {
            throw ServiceException(ErrorCode.SEAT_HELD_BY_OTHER_USER)
        }

        cleanupRedis(redisKey)

        val seat = scheduleSeatRepository.findWithLockByScheduleIdAndSeatNumber(scheduleId, seatNumber)
        seat?.releaseToAvailable()

        // 트랜잭션 커밋 후 SSE 브로드캐스트 (SeatStatusSseBroadcaster.onSeatReleased)
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

    fun cleanupRedis(redisKey: String) = redissonClient.getMap<String, String>(redisKey, StringCodec.INSTANCE).delete()

    fun validateSeatHolds(userId: Long, concertId: Long, scheduleId: Long, seatHolds: List<SeatHoldInfo>) {
        for (hold in seatHolds) {
            val redisKey = generateSeatOccupyKey(concertId, scheduleId, hold.seatNumber)
            val hashMap = redissonClient.getMap<String, String>(redisKey, StringCodec.INSTANCE)

            val holdUserId = hashMap["userId"]
            val holdOccupyToken = hashMap["occupyToken"]

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

    fun cancelDelayedQueueMessage(concertId: Long, scheduleId: Long, seatNumber: String) {
        try {
            val message = SeatOccupiedEventListener.buildMessage(concertId, scheduleId, seatNumber)
            val blockingQueue = redissonClient.getBlockingQueue<String>(SeatHoldExpiredHandler.DELAYED_QUEUE_KEY, StringCodec.INSTANCE)
            val delayedQueue = redissonClient.getDelayedQueue(blockingQueue)
            delayedQueue.remove(message)
        } catch (e: Exception) {
            log.warn("Delayed Queue 메시지 제거 실패 (무시됨): {}", e.message)
        }
    }

    companion object {
        const val OCCUPY_TTL_SECONDS: Long = 600

        private val OCCUPY_SCRIPT = """
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

        @JvmStatic
        fun generateSeatOccupyKey(concertId: Long, scheduleId: Long, seatNumber: String): String =
            "seat:occupy:$concertId:$scheduleId:$seatNumber"
    }
}
