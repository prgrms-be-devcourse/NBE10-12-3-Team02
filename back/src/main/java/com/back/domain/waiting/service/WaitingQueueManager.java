package com.back.domain.waiting.service;

import com.back.domain.waiting.dto.QueueStatusDto;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import java.time.Duration;
import java.util.List;
import java.util.Set;
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
        redisTemplate.opsForSet().add("queue:active:schedules", scheduleId.toString());

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

    public boolean cancelWaiting(Long scheduleId, Long userId) {
        String waitKey = generateWaitKey(scheduleId);
        String user = userId.toString();

        Long removed = redisTemplate.opsForZSet().remove(waitKey, user);
        return removed != null && removed > 0;
    }

    public boolean cancelActiveUser(Long scheduleId, Long userId) {
        String activeKey = generateQueueActiveKey(scheduleId);
        String user = userId.toString();

        Long removed = redisTemplate.opsForZSet().remove(activeKey, user);
        redisTemplate.delete(generateActiveTokenKey(scheduleId, userId));
        return removed != null && removed > 0;
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
    public long removeExpiredActiveUsers(Long scheduleId) {
        String activeKey = generateQueueActiveKey(scheduleId);
        Long removed = redisTemplate.opsForZSet()
                .removeRangeByScore(activeKey, 0, System.currentTimeMillis());
        return removed == null ? 0L : removed;
    }

    public static String generateActiveTokenKey(
            Long scheduleId,
            Long userId
    ) {
        return ACTIVE_TOKEN_KEY_PREFIX + scheduleId + ":" + userId;
    }

    public static String generateQueueActiveKey(Long scheduleId) {
        return "queue:active:schedule:%d".formatted(scheduleId);
    }

    public void removeActiveUser(Long scheduleId, Long userId) {
        redisTemplate.opsForZSet().remove(
                generateQueueActiveKey(scheduleId),
                userId.toString()
        );
        redisTemplate.delete(generateActiveTokenKey(scheduleId, userId));
    }


        public List<Long> getRemainingUserIds(Long scheduleId) {
        Set<String> userIds = redisTemplate.opsForZSet()
                .range(generateWaitKey(scheduleId), 0, -1);

        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return userIds.stream()
                .map(Long::valueOf)
                .toList();
    }

    public List<Long> addActiveUser(Long scheduleId, long capacity, int batchSize, Duration ttl) {
        String waitKey = generateWaitKey(scheduleId);
        String activeKey = generateQueueActiveKey(scheduleId);
        long now = System.currentTimeMillis();
        long expiredAt = now + ttl.toMillis();

        List<String> userIds = redisTemplate.execute(
                ADMIT_USERS_SCRIPT,
                List.of(waitKey, activeKey),
                String.valueOf(capacity),
                String.valueOf(batchSize),
                String.valueOf(now),
                String.valueOf(expiredAt)
        );

        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream().map(Long::valueOf).toList();
    }

    private static final RedisScript<List> ADMIT_USERS_SCRIPT = new DefaultRedisScript<>(
            """                                                                                                                                                                                                                                                                                                               
                    redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[3])
                    
                    local activeCount = redis.call('ZCARD', KEYS[2])
                    local availableSlots = tonumber(ARGV[1]) - activeCount
                    if availableSlots <= 0 then
                      return {}
                    end
                    
                    local popCount = math.min(availableSlots, tonumber(ARGV[2]))
                    local users = redis.call('ZRANGE', KEYS[1], 0, popCount - 1)
                    if #users == 0 then
                      return {}
                    end
                    
                    redis.call('ZREM', KEYS[1], unpack(users))
                    for _, u in ipairs(users) do
                      redis.call('ZADD', KEYS[2], ARGV[4], u)
                    end
                    
                    return users
                    """,
            List.class
    );
    public String issueToken(Long scheduleId, Long userId, Duration ttl) {
        String entryToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                generateActiveTokenKey(scheduleId, userId),
                entryToken,
                ttl
        );
        return entryToken;
    }

    public QueueStatusDto getQueueStatus(Long scheduleId) {
        String waitKey = generateWaitKey(scheduleId);

        Long totalWaitingCount = redisTemplate.opsForZSet().zCard(waitKey);
        if (totalWaitingCount == null || totalWaitingCount == 0) {
            return new QueueStatusDto(0L, 0L);
        }

        Set<TypedTuple<String>> range = redisTemplate.opsForZSet().rangeWithScores(waitKey, 0, 0);
        if (range == null || range.isEmpty()) {
            return new QueueStatusDto(0L, totalWaitingCount);
        }
        Double firstScore = range.iterator().next().getScore();
        long currentAllowedSequence = (firstScore != null) ? firstScore.longValue() - 1 : 0L;

        return new QueueStatusDto(currentAllowedSequence, totalWaitingCount);
    }

    public Long getQueueSequence(Long scheduleId, Long userId) {
        String waitKey = generateWaitKey(scheduleId);
        Double score = redisTemplate.opsForZSet().score(waitKey, userId.toString());

        return (score != null) ? score.longValue() : 0L;
    }

    private static final RedisScript<String> GET_ACTIVE_TOKEN_SCRIPT = new DefaultRedisScript<>(
            """
                    local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
                    if score and tonumber(score) > tonumber(ARGV[2]) then
                        return redis.call('GET', KEYS[2])
                    end
                    return nil
                    """,
            String.class
    );

    public String getActiveToken(Long scheduleId, Long userId) {
        String activeKey = generateQueueActiveKey(scheduleId);
        String tokenKey = generateActiveTokenKey(scheduleId, userId);

        return redisTemplate.execute(
                GET_ACTIVE_TOKEN_SCRIPT,
                List.of(activeKey, tokenKey),
                userId.toString(),
                String.valueOf(System.currentTimeMillis())
        );
    }

    public void clearWaitingQueue(Long scheduleId) {
        String waitKey = generateWaitKey(scheduleId);
        redisTemplate.delete(waitKey);
    }
}
