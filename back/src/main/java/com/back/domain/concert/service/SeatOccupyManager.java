package com.back.domain.concert.service;

import com.back.domain.concert.dto.SeatOccupyResponse;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SeatOccupyManager {
    private final ConcertService concertService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final long OCCUPY_TTL_SECONDS = 600;
    private static final RedisScript<Long> OCCUPY_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('EXISTS', KEYS[1]) == 0 then
              redis.call('HSET', KEYS[1], 'userId', ARGV[1], 'occupyToken', ARGV[2])
              redis.call('EXPIRE', KEYS[1], ARGV[3])
              return 1
            else
              return 0
            end
            """,
            Long.class
    );

    public SeatOccupyResponse seatOccupy(Long concertId, Long scheduleId, String seatNumber, Long userId) {
        concertService.validateConcertScheduleMatch(concertId, scheduleId);

        String redisKey = String.format("seat:occupy:%d:%d:%s", concertId, scheduleId, seatNumber);
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

        try {
            concertService.updateSeatStatusToHold(scheduleId, seatNumber);
        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw new ServiceException(ErrorCode.CONCERT_SEAT_HOLD_FAILED);
        }

        return SeatOccupyResponse.of(occupyToken, OCCUPY_TTL_SECONDS);
    }
}
