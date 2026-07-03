package com.back.global.security.jwt.repository;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.security.jwt.RefreshTokenKeyType;
import com.back.global.security.jwt.RefreshTokenLuaScripts;
import com.back.global.security.jwt.RefreshTokenRotateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
    private final StringRedisTemplate redisTemplate;

    @Value("${custom.redis.refresh-token.prefix}")
    private String prefix;

    @Value("${custom.redis.refresh-token.index-prefix}")
    private String indexPrefix;

    public RefreshTokenRotateResult rotate(
            Long userId,
            String oldJti,
            String requestRefreshTokenHash,
            String newJti,
            String newRefreshTokenHash,
            Duration ttl
    ) {
        Long result = redisTemplate.execute(
                RefreshTokenLuaScripts.ROTATE,
                List.of(
                        generateKey(RefreshTokenKeyType.TOKEN, userId, oldJti),
                        generateKey(RefreshTokenKeyType.TOKEN, userId, newJti),
                        generateKey(RefreshTokenKeyType.INDEX, userId, null)
                ),
                requestRefreshTokenHash,
                newRefreshTokenHash,
                String.valueOf(ttl.toSeconds()),
                oldJti,
                newJti
        );

        if (result == null) {
            throw new ServiceException(ErrorCode.AUTH_REFRESH_TOKEN_ROTATION_FAILED);
        }

        return switch (Math.toIntExact(result)) {
            case 1 -> RefreshTokenRotateResult.SUCCESS;
            case -1 -> RefreshTokenRotateResult.MISMATCH;
            case 0 -> RefreshTokenRotateResult.NOT_FOUND;
            default -> throw new ServiceException(ErrorCode.AUTH_REFRESH_TOKEN_ROTATION_FAILED);
        };
    }

    public void save(Long userId, String jti, String refreshTokenHash, Duration ttl) {
        String key = generateKey(RefreshTokenKeyType.TOKEN, userId, jti);
        String indexKey = generateKey(RefreshTokenKeyType.INDEX, userId, null);

        redisTemplate.opsForValue().set(key, refreshTokenHash, ttl);
        redisTemplate.opsForSet().add(indexKey, jti);
        redisTemplate.expire(indexKey, ttl);
    }

    public void delete(Long userId, String jti) {
        redisTemplate.delete(generateKey(RefreshTokenKeyType.TOKEN, userId, jti));
        redisTemplate.opsForSet().remove(generateKey(RefreshTokenKeyType.INDEX, userId, null), jti);
    }

    public void deleteAllByUserId(Long userId) {
        String indexKey = generateKey(RefreshTokenKeyType.INDEX, userId, null);
        Set<String> jtis = redisTemplate.opsForSet().members(indexKey);

        if (jtis == null || jtis.isEmpty()) {
            return;
        }

        List<String> keys = jtis.stream()
                .map(jti -> generateKey(RefreshTokenKeyType.TOKEN, userId, jti))
                .toList();

        redisTemplate.delete(keys);
        redisTemplate.delete(indexKey);
    }

    private String generateKey(RefreshTokenKeyType type, Long userId, String jti) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        return switch (type) {
            case TOKEN -> {
                if (jti == null || jti.isBlank()) {
                    throw new IllegalArgumentException("jti is required for refresh token key");
                }

                yield prefix + userId + ":" + jti;
            }

            case INDEX -> {
                if (jti != null && !jti.isBlank()) {
                    throw new IllegalArgumentException("jti must be empty for refresh token index key");
                }

                yield indexPrefix + userId;
            }
        };
    }
}
