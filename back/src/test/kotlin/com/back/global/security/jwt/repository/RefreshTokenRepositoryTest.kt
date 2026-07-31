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
    @DisplayName("정상 토큰 저장 및 verify 검증")
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
    @DisplayName("rotate 성공 시 기존 JTI는 5초 임시 만료가 설정되고, 신규 JTI가 저장되며 Index 세트가 갱신된다")
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
    @DisplayName("rotate 시 요청 Hash와 저장된 Hash가 다르면 MISMATCH가 반환된다")
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
    @DisplayName("rotate 시 존재하지 않는 JTI로 요청하면 NOT_FOUND가 반환된다")
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
    @DisplayName("deleteAllByUserId 호출 시 사용자의 모든 Refresh Token과 Index 세트가 삭제된다")
    fun deleteAllByUserId() {
        refreshTokenRepository.save(userId, "jti-1", "hash-1", ttl)
        refreshTokenRepository.save(userId, "jti-2", "hash-2", ttl)

        refreshTokenRepository.deleteAllByUserId(userId)

        assertThat(refreshTokenRepository.verify(userId, "jti-1", "hash-1")).isEqualTo(RefreshTokenValidationResult.NOT_FOUND)
        assertThat(refreshTokenRepository.verify(userId, "jti-2", "hash-2")).isEqualTo(RefreshTokenValidationResult.NOT_FOUND)
    }
}
