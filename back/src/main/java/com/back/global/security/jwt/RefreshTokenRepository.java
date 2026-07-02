package com.back.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "auth:refresh:";
    private static final String INDEX_PREFIX = "auth:refresh-index:";

    public void save(Long userId, String jti, String refreshTokenHash, Duration ttl) {
        String key = getKey(userId, jti);
        String indexKey = getIndexKey(userId);

        redisTemplate.opsForValue().set(key, refreshTokenHash, ttl);
        redisTemplate.opsForSet().add(indexKey, jti);
        redisTemplate.expire(indexKey, ttl);
    }

    public String find(Long userId, String jti) {
        String key = getKey(userId, jti);
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            redisTemplate.opsForSet().remove(getIndexKey(userId), jti);
        }

        return value;
    }

    public void delete(Long userId, String jti) {
        redisTemplate.delete(getKey(userId, jti));
        redisTemplate.opsForSet().remove(getIndexKey(userId), jti);
    }

    public void deleteAllByUserId(Long userId) {
        String indexKey = getIndexKey(userId);
        Set<String> jtis = redisTemplate.opsForSet().members(indexKey);

        if (jtis == null || jtis.isEmpty()) {
            return;
        }

        List<String> keys = jtis.stream()
                .map(jti -> getKey(userId, jti))
                .toList();

        redisTemplate.delete(keys);
        redisTemplate.delete(indexKey);
    }

    private String getIndexKey(Long userId) {
        return INDEX_PREFIX + userId;
    }

    private String getKey(Long userId, String jti) {
        return PREFIX + userId + ":" + jti;
    }
}