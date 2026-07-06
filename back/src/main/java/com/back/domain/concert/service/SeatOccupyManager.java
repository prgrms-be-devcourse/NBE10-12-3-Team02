package com.back.domain.concert.service;

import com.back.domain.concert.dto.SeatOccupyResponse;
import com.back.domain.concert.dto.SeatSelectionResponse;
import com.back.domain.concert.dto.SeatSelectionResponse.SeatDetailResponse;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.entity.SeatStatus;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class SeatOccupyManager {
    private final ConcertService concertService;
    private final StringRedisTemplate redisTemplate;

    private static final long OCCUPY_TTL_SECONDS = 600;
    private static final RedisScript<Long> OCCUPY_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('EXISTS', KEYS[1]) == 1 then
              if redis.call('HGET', KEYS[1], 'userId') == ARGV[1] then
                redis.call('HSET', KEYS[1], 'occupyToken', ARGV[2])
                redis.call('EXPIRE', KEYS[1], ARGV[3])
                return 1
              else
                return 0
              end
            else
              redis.call('HSET', KEYS[1], 'userId', ARGV[1], 'occupyToken', ARGV[2])
              redis.call('EXPIRE', KEYS[1], ARGV[3])
              return 1
            end
            """,
            Long.class
    );

    public SeatOccupyResponse seatOccupy(Long concertId, Long scheduleId, String seatNumber, Long userId) {
        concertService.validateConcertScheduleMatch(concertId, scheduleId);
        concertService.validateSeatAvailable(scheduleId, seatNumber);

        String redisKey = generateSeatOccupyKey(concertId, scheduleId, seatNumber);
        String occupyToken = UUID.randomUUID().toString();

        Long result = redisTemplate.execute(
                OCCUPY_SCRIPT,
                List.of(redisKey),
                userId.toString(),
                occupyToken,
                String.valueOf(OCCUPY_TTL_SECONDS)
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
    }

    public SeatSelectionResponse getSeatSelection(Long concertId, Long scheduleId, Long userId) {
        concertService.validateConcertScheduleMatch(concertId, scheduleId);
        List<ScheduleSeat> seats = concertService.getScheduleSeats(scheduleId);

        List<Object> existsResults = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (var seat : seats) {
                String key = generateSeatOccupyKey(concertId, scheduleId, seat.getSeatNumber());
                byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);
                connection.keyCommands().exists(rawKey);
            }
            return null;
        });

        Map<String, Integer> pricesMap = concertService.convertToPriceMap(seats);

        List<SeatDetailResponse> seatResponses = IntStream.range(0, seats.size())
                .mapToObj(i -> {
                    ScheduleSeat seat = seats.get(i);
                    SeatStatus status = seat.getSeatStatus();
                    Object res = existsResults.get(i);
                    boolean isHold = status == SeatStatus.AVAILABLE &&
                            (Boolean.TRUE.equals(res) || (res instanceof Number n && n.longValue() > 0));

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
}
