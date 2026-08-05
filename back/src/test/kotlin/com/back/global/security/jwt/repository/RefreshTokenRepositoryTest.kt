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
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisTestConfig::class)
class RefreshTokenRepositoryTest {

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    private val userId = 999L
    private val sessionId = "session-uuid-1"
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
    @DisplayName("세션 Family에 Refresh Token을 저장하고 검증한다")
    fun t1() {
        refreshTokenRepository.save(userId, sessionId, oldJti, oldHash, ttl)

        assertThat(refreshTokenRepository.verify(userId, sessionId, oldJti, oldHash))
            .isEqualTo(RefreshTokenValidationResult.SUCCESS)
        assertThat(refreshTokenRepository.verify(userId, sessionId, oldJti, "wrong-hash"))
            .isEqualTo(RefreshTokenValidationResult.MISMATCH)
        assertThat(refreshTokenRepository.verify(userId, sessionId, "missing-jti", oldHash))
            .isEqualTo(RefreshTokenValidationResult.NOT_FOUND)
    }

    @Test
    @DisplayName("Rotation 시 이전 토큰을 USED로 남기고 새 토큰을 ACTIVE로 저장한다")
    fun t2() {
        refreshTokenRepository.save(userId, sessionId, oldJti, oldHash, ttl)

        val result = rotate(oldHash)

        assertThat(result).isEqualTo(RefreshTokenValidationResult.SUCCESS)
        assertThat(refreshTokenRepository.verify(userId, sessionId, newJti, newHash))
            .isEqualTo(RefreshTokenValidationResult.SUCCESS)
    }

    @Test
    @DisplayName("이미 사용된 Refresh Token을 재사용하면 Family 전체를 폐기한다")
    fun t3() {
        refreshTokenRepository.save(userId, sessionId, oldJti, oldHash, ttl)
        assertThat(rotate(oldHash)).isEqualTo(RefreshTokenValidationResult.SUCCESS)

        val reusedResult = rotate(oldHash)

        assertThat(reusedResult).isEqualTo(RefreshTokenValidationResult.REUSED)
        assertThat(refreshTokenRepository.verify(userId, sessionId, newJti, newHash))
            .isEqualTo(RefreshTokenValidationResult.NOT_FOUND)
    }

    @Test
    @DisplayName("한 세션의 토큰 재사용은 다른 로그인 세션을 폐기하지 않는다")
    fun t4() {
        val otherSessionId = "session-uuid-2"
        refreshTokenRepository.save(userId, sessionId, oldJti, oldHash, ttl)
        refreshTokenRepository.save(userId, otherSessionId, "other-jti", "other-hash", ttl)
        rotate(oldHash)

        assertThat(rotate(oldHash)).isEqualTo(RefreshTokenValidationResult.REUSED)
        assertThat(refreshTokenRepository.verify(userId, otherSessionId, "other-jti", "other-hash"))
            .isEqualTo(RefreshTokenValidationResult.SUCCESS)
    }

    @Test
    @DisplayName("사용자의 모든 Refresh Token Family를 삭제한다")
    fun t5() {
        refreshTokenRepository.save(userId, sessionId, oldJti, oldHash, ttl)
        refreshTokenRepository.save(userId, "session-uuid-2", "other-jti", "other-hash", ttl)

        refreshTokenRepository.deleteAllByUserId(userId)

        assertThat(refreshTokenRepository.verify(userId, sessionId, oldJti, oldHash))
            .isEqualTo(RefreshTokenValidationResult.NOT_FOUND)
        assertThat(refreshTokenRepository.verify(userId, "session-uuid-2", "other-jti", "other-hash"))
            .isEqualTo(RefreshTokenValidationResult.NOT_FOUND)
    }

    private fun rotate(requestHash: String): RefreshTokenValidationResult =
        refreshTokenRepository.rotate(
            userId = userId,
            sessionId = sessionId,
            oldJti = oldJti,
            requestRefreshTokenHash = requestHash,
            newJti = newJti,
            newRefreshTokenHash = newHash,
            ttl = ttl,
        )
}
