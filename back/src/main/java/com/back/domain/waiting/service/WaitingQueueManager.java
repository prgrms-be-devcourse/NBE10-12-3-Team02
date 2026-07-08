package com.back.domain.waiting.service;

import com.back.domain.waiting.dto.ActiveEntry;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.security.interceptor.QueueInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class WaitingQueueManager {
    private final StringRedisTemplate redisTemplate;
    private static final String WAIT_KEY_PREFIX = "queue:wait:schedule:";
    private static final String SEQUENCE_KEY_PREFIX = "queue:wait:sequence:schedule:";
    private static final String ACTIVE_TOKEN_KEY_PREFIX = "queue:active:token:";

    public Long registerWaiting(Long scheduleId, Long userId) {
        String waitKey = generateWaitKey(scheduleId);
        String seqKey = generateSequenceKey(scheduleId);
        String user = userId.toString();

        Long rank = redisTemplate.execute(
                REGISTER_WAITING_SCRIPT,
                List.of(waitKey, seqKey),
                user
        );

        if (rank == null || rank < 1) {
            throw new ServiceException(ErrorCode.WAITING_QUEUE_REGISTER_FAILED);
        }

        return rank;
    }

    public Long showWaitingRank(Long scheduleId, Long userId) {
        String waitKey = generateWaitKey(scheduleId);
        String user = userId.toString();

        Long rank = redisTemplate.opsForZSet()
                .rank(waitKey, user);

        if (rank == null) {
            throw new ServiceException(ErrorCode.WAITING_QUEUE_NOT_FOUND);
        }

        return rank + 1;
    }

    public void cancelWaiting(Long scheduleId, Long userId) {
        String waitKey = generateWaitKey(scheduleId);
        String user = userId.toString();

        Long removedRank = redisTemplate.opsForZSet().remove(waitKey, user);

        if (removedRank == null || removedRank == 0L) {
            throw new ServiceException(ErrorCode.WAITING_QUEUE_NOT_FOUND);
        }
    }

    private String generateWaitKey(Long scheduleId) {
        return WAIT_KEY_PREFIX + scheduleId;
    }

    private String generateSequenceKey(Long scheduleId) {
        return SEQUENCE_KEY_PREFIX + scheduleId;
    }

    private static final RedisScript<Long> REGISTER_WAITING_SCRIPT = new DefaultRedisScript<>(
            """
            local exists = redis.call('ZSCORE', KEYS[1], ARGV[1])
  
            if not exists then
              local sequence = redis.call('INCR', KEYS[2])
              redis.call('ZADD', KEYS[1], sequence, ARGV[1])
            end
  
            local rank = redis.call('ZRANK', KEYS[1], ARGV[1])
  
            if not rank then
              return -1
            end
  
            return rank + 1
            """,
            Long.class
    );
    public List<Long> popUsers(Long scheduleId, int count) {
        String waitKey = generateWaitKey(scheduleId);

        List<String> userIds = redisTemplate.execute(
                POP_USERS_SCRIPT,
                List.of(waitKey),
                String.valueOf(count)
        );

        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return userIds.stream()
                .map(Long::valueOf)
                .toList();
    }

    private static final RedisScript<List> POP_USERS_SCRIPT = new DefaultRedisScript<>(
            """
            local users = redis.call('ZRANGE', KEYS[1], 0, tonumber(ARGV[1]) - 1)
  
            if #users == 0 then
              return {}
            end
  
            redis.call('ZREM', KEYS[1], unpack(users))
  
            return users
            """,
            List.class
    );

    public long countActiveUsers(Long scheduleId) {
        removeExpiredActiveUsers(scheduleId);
        String activeKey = QueueInterceptor.generateQueueActiveKey(scheduleId);
        Long size = redisTemplate.opsForZSet()
                .zCard(activeKey);

        return size == null ? 0L : size;
    }

    public long removeExpiredActiveUsers(Long scheduleId) {
        String activeKey = QueueInterceptor.generateQueueActiveKey(scheduleId);
        Long removed = redisTemplate.opsForZSet()
                .removeRangeByScore(activeKey, 0, System.currentTimeMillis());
        return removed == null ? 0L : removed;
    }

    public ActiveEntry addActiveUser(
            Long scheduleId,
            Long userId,
            Duration ttl
    ) {
        String entryToken = UUID.randomUUID().toString();
        long expiredAt = System.currentTimeMillis() + ttl.toMillis();

        redisTemplate.opsForZSet().add(
                QueueInterceptor.generateQueueActiveKey(scheduleId),
                entryToken,
                expiredAt
        );

        redisTemplate.opsForValue().set(
                generateActiveTokenKey(scheduleId, userId),
                entryToken,
                ttl
        );

        return new ActiveEntry(entryToken, expiredAt);
    }
    public static String generateActiveTokenKey(
            Long scheduleId,
            Long userId
    ) {
        return ACTIVE_TOKEN_KEY_PREFIX + scheduleId + ":" + userId;
    }
    public void removeActiveUser(
            Long scheduleId,
            Long userId
    ) {
        String activeTokenKey = generateActiveTokenKey(scheduleId, userId);

        String entryToken = redisTemplate.opsForValue()
                .get(activeTokenKey);

        if (entryToken == null) {
            return;
        }

        redisTemplate.opsForZSet()
                .remove(
                        QueueInterceptor.generateQueueActiveKey(scheduleId),
                        entryToken
                );

        redisTemplate.delete(activeTokenKey);
    }
    public long getRemainingCount(Long scheduleId) {
        Long count = redisTemplate.opsForZSet().zCard(generateWaitKey(scheduleId));
        return count == null ? 0L : count;
    }
}
