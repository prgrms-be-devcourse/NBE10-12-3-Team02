package com.back.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "auth:refresh:";

    public void save(Long userId, String jti, String refreshTokenHash, Duration ttl) {
        String key = getKey(userId, jti);

        redisTemplate.opsForValue().set(
                key,
                refreshTokenHash,
                ttl
        );
    }

    public String find(Long userId, String jti) {
        String key = getKey(userId, jti);

        return redisTemplate.opsForValue().get(key);
    }

    public void delete(Long userId, String jti) {
        String key = getKey(userId, jti);

        redisTemplate.delete(key);
    }

    public void deleteAllByUserId(Long userId) {
        Set<String> keys = redisTemplate.keys(PREFIX + userId + ":*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        redisTemplate.delete(keys);
    }

    private String getKey(Long userId, String jti) {
        return PREFIX + userId + ":" + jti;
    }
}