package com.back.global.security.jwt.repository

import com.back.global.RedisTestConfig
import com.back.global.security.jwt.constant.RefreshTokenValidationResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisTestConfig::class)
class RefreshTokenRepositoryTest {

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    private val userId = 999L
    private val oldJti = "old-jti-uuid-1"
    private val oldHash = "old-token-hash-value-1"
    private val newJti = "new-jti-uuid-2"
    private val newHash = "new-token-hash-value-2"
    private val ttl = Duration.ofMinutes(10)

    @BeforeEach
    fun setUp() {
        refreshTokenRepository.deleteAllByUserId(userId)
    }

    @Test
    @DisplayName("토큰 저장 및 검증")
    fun saveAndVerify() {
        refreshTokenRepository.save(userId, oldJti, oldHash, ttl)

        val resultSuccess = refreshTokenRepository.verify(userId, oldJti, oldHash)
        assertThat(resultSuccess).isEqualTo(RefreshTokenValidationResult.SUCCESS)

        val resultMismatch = refreshTokenRepository.verify(userId, oldJti, "wrong-hash")
        assertThat(resultMismatch).isEqualTo(RefreshTokenValidationResult.MISMATCH)

        val resultNotFound = refreshTokenRepository.verify(userId, "non-existent-jti", oldHash)
        assertThat(resultNotFound).isEqualTo(RefreshTokenValidationResult.NOT_FOUND)
    }

    @Test
    @DisplayName("토큰 교체(rotate) 성공")
    fun rotate_success() {
        refreshTokenRepository.save(userId, oldJti, oldHash, ttl)

        val result = refreshTokenRepository.rotate(
            userId = userId,
            oldJti = oldJti,
            requestRefreshTokenHash = oldHash,
            newJti = newJti,
            newRefreshTokenHash = newHash,
            ttl = ttl
        )

        assertThat(result).isEqualTo(RefreshTokenValidationResult.SUCCESS)

        // New JTI should be active
        val newVerify = refreshTokenRepository.verify(userId, newJti, newHash)
        assertThat(newVerify).isEqualTo(RefreshTokenValidationResult.SUCCESS)

        // Old JTI has a short grace period TTL (<= 5s) set by Lua script for network race conditions
        val expireSeconds = stringRedisTemplate.getExpire("auth:refresh:$userId:$oldJti")
        assertThat(expireSeconds).isGreaterThan(0L).isLessThanOrEqualTo(5L)
    }

    @Test
    @DisplayName("토큰 불일치 시 MISMATCH 반환")
    fun rotate_mismatch() {
        refreshTokenRepository.save(userId, oldJti, oldHash, ttl)

        val result = refreshTokenRepository.rotate(
            userId = userId,
            oldJti = oldJti,
            requestRefreshTokenHash = "tampered-hash",
            newJti = newJti,
            newRefreshTokenHash = newHash,
            ttl = ttl
        )

        assertThat(result).isEqualTo(RefreshTokenValidationResult.MISMATCH)
    }

    @Test
    @DisplayName("존재하지 않는 토큰 교체 시 NOT_FOUND 반환")
    fun rotate_notFound() {
        val result = refreshTokenRepository.rotate(
            userId = userId,
            oldJti = "non-existent-jti",
            requestRefreshTokenHash = oldHash,
            newJti = newJti,
            newRefreshTokenHash = newHash,
            ttl = ttl
        )

        assertThat(result).isEqualTo(RefreshTokenValidationResult.NOT_FOUND)
    }

    @Test
    @DisplayName("사용자 토큰 전체 삭제")
    fun deleteAllByUserId() {
        refreshTokenRepository.save(userId, "jti-1", "hash-1", ttl)
        refreshTokenRepository.save(userId, "jti-2", "hash-2", ttl)

        refreshTokenRepository.deleteAllByUserId(userId)

        assertThat(refreshTokenRepository.verify(userId, "jti-1", "hash-1")).isEqualTo(RefreshTokenValidationResult.NOT_FOUND)
        assertThat(refreshTokenRepository.verify(userId, "jti-2", "hash-2")).isEqualTo(RefreshTokenValidationResult.NOT_FOUND)
    }
}
