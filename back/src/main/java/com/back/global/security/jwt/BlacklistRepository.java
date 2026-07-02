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
        String tokenHash = TokenHashUtil.sha256(accessToken);
        redisTemplate.opsForValue().set(PREFIX + tokenHash, "blacklisted", ttl);
    }

    public boolean isBlacklisted(String accessToken) {
        String tokenHash = TokenHashUtil.sha256(accessToken);
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + tokenHash));
    }
}