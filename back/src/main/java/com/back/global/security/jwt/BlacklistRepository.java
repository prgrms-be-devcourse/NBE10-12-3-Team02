package com.back.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class BlacklistRepository {
    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "auth:blacklist:";

    public void add(String accessToken, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + accessToken, "blacklisted", ttl);
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + accessToken));
    }
}