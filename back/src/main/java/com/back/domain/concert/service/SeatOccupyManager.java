package com.back.domain.concert.service;

import com.back.domain.concert.dto.SeatOccupyResponse;
import com.back.domain.concert.dto.SeatSelectionResponse;
import com.back.domain.concert.dto.SeatSelectionResponse.SeatDetailResponse;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.entity.SeatStatus;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SeatOccupyManager {
    private final ConcertService concertService;
    private final TicketRepository ticketRepository;
    private final StringRedisTemplate redisTemplate;

    private static final long OCCUPY_TTL_SECONDS = 600;
    private static final RedisScript<Long> OCCUPY_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('EXISTS', KEYS[1]) == 1 then
              if redis.call('HGET', KEYS[1], 'userId') == ARGV[1] then
                redis.call('HSET', KEYS[1], 'occupyToken', ARGV[2])
                redis.call('EXPIRE', KEYS[1], ARGV[3])
                redis.call('ZADD', KEYS[2], ARGV[5], ARGV[4])
                return 1
              else
                return 0
              end
            else
              redis.call('HSET', KEYS[1], 'userId', ARGV[1], 'occupyToken', ARGV[2])
              redis.call('EXPIRE', KEYS[1], ARGV[3])
              redis.call('ZADD', KEYS[2], ARGV[5], ARGV[4])
              return 1
            end
            """,
            Long.class
    );

    public SeatOccupyResponse seatOccupy(Long concertId, Long scheduleId, String seatNumber, Long userId) {
        concertService.validateConcertScheduleMatch(concertId, scheduleId);
        concertService.validateScheduleBookable(scheduleId);
        concertService.validateSeatAvailable(scheduleId, seatNumber);

        String redisKey = generateSeatOccupyKey(concertId, scheduleId, seatNumber);
        String indexKey = generateSeatOccupyIndexKey(concertId, scheduleId);
        String occupyToken = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long expireAt = now + (OCCUPY_TTL_SECONDS * 1000);

        Long result = redisTemplate.execute(
                OCCUPY_SCRIPT,
                List.of(redisKey, indexKey),
                userId.toString(),
                occupyToken,
                String.valueOf(OCCUPY_TTL_SECONDS),
                seatNumber,
                String.valueOf(expireAt)
        );

        if (result == null || result == 0L) {
            throw new ServiceException(ErrorCode.SEAT_HELD_BY_OTHER_USER);
        }

        return SeatOccupyResponse.of(occupyToken, OCCUPY_TTL_SECONDS);
    }

    public void seatOccupyCancel(Long concertId, Long scheduleId, String seatNumber, Long userId) {
        concertService.validateConcertScheduleMatch(concertId, scheduleId);

        String redisKey = generateSeatOccupyKey(concertId, scheduleId, seatNumber);

        String occupyUserId = (String) redisTemplate.opsForHash().get(redisKey, "userId");
        if (occupyUserId == null) {
            throw new ServiceException(ErrorCode.SEAT_HOLD_EXPIRED);
        }
        if (!occupyUserId.equals(userId.toString())) {
            throw new ServiceException(ErrorCode.SEAT_HELD_BY_OTHER_USER);
        }

        redisTemplate.delete(redisKey);

        String indexKey = generateSeatOccupyIndexKey(concertId, scheduleId);
        redisTemplate.opsForZSet().remove(indexKey, seatNumber);
    }

    public SeatSelectionResponse getSeatSelection(Long concertId, Long scheduleId, Long userId) {
        concertService.validateConcertScheduleMatch(concertId, scheduleId);
        concertService.validateScheduleBookable(scheduleId);
        long currentTicketCount = ticketRepository.countByUser_UserIdAndSchedule_ScheduleIdAndIsValidTrue(userId, scheduleId);
        if (currentTicketCount >= 3) {
            throw new ServiceException(ErrorCode.EXCEED_TICKET_LIMIT);
        }
        List<ScheduleSeat> seats = concertService.getScheduleSeats(scheduleId);

        String indexKey = generateSeatOccupyIndexKey(concertId, scheduleId);
        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().removeRangeByScore(indexKey, 0, now);
        Set<String> occupiedSeats = redisTemplate.opsForZSet().rangeByScore(indexKey, now, Double.MAX_VALUE);

        Map<String, Integer> pricesMap = concertService.convertToPriceMap(seats);

        List<SeatDetailResponse> seatResponses = seats.stream()
                .map(seat -> {
                    SeatStatus status = seat.getSeatStatus();
                    boolean isHold = status == SeatStatus.AVAILABLE &&
                            occupiedSeats != null && occupiedSeats.contains(seat.getSeatNumber());

                    return new SeatDetailResponse(
                            seat.getSeatNumber(),
                            isHold ? SeatStatus.HOLD : status,
                            seat.getGradeName()
                    );
                })
                .toList();

        return SeatSelectionResponse.of(concertId, scheduleId, pricesMap, seatResponses);
    }

    public static String generateSeatOccupyKey(Long concertId, Long scheduleId, String seatNumber) {
        return "seat:occupy:%d:%d:%s".formatted(concertId, scheduleId, seatNumber);
    }

    public static String generateSeatOccupyIndexKey(Long concertId, Long scheduleId) {
        return "seat:occupy:index:%d:%d".formatted(concertId, scheduleId);
    }
}
