package com.back.global.security.email

import com.back.global.RedisTestConfig
import com.back.global.security.email.constant.EmailVerificationConfirmResult
import com.back.global.security.jwt.TokenHashUtil
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisTestConfig::class)
class EmailVerificationRepositoryTest {

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    private lateinit var repository: EmailVerificationRepository

    @BeforeEach
    fun setUp() {
        repository = EmailVerificationRepository(
            stringRedisTemplate,
            EmailVerificationProperties(
                codeExpirationSeconds = CODE_TTL.seconds,
                verifiedExpirationSeconds = VERIFICATION_TTL.seconds,
                resendCooldownSeconds = COOLDOWN_TTL.seconds,
                maxAttempts = MAX_ATTEMPTS,
                redisPrefix = PREFIX,
            ),
        )

        val keys = stringRedisTemplate.keys("$PREFIX*")
        if (!keys.isNullOrEmpty()) {
            stringRedisTemplate.delete(keys)
        }
    }

    @Test
    @DisplayName("최초 인증번호 생성 시 코드가 저장되고 쿨다운/시도횟수 키가 생성된다")
    fun saveChallenge_success() {
        val created = saveChallenge()

        assertThat(created).isTrue()
        assertThat(stringValue(codeKey())).isEqualTo(CODE_HASH)
        assertThat(stringValue(cooldownKey())).isEqualTo("1")
        assertThat(stringValue(attemptKey())).isNull()
        assertThat(remainingTime(codeKey())).isGreaterThan(0L)
        assertThat(remainingTime(cooldownKey())).isGreaterThan(0L)
    }

    @Test
    @DisplayName("쿨다운 시간이 남아있으면 인증번호 재전송이 차단된다")
    fun saveChallenge_blockedByCooldown() {
        saveChallenge()

        val reattempt = saveChallenge()

        assertThat(reattempt).isFalse()
    }

    @Test
    @DisplayName("올바른 인증번호 검증 시 성공하고 검증 완료 토큰 상태가 저장된다")
    fun confirm_success() {
        saveChallenge()

        val result = repository.confirm(
            EMAIL_HASH,
            CODE_HASH,
            TOKEN_HASH,
            MAX_ATTEMPTS,
            VERIFICATION_TTL,
        )

        assertThat(result).isEqualTo(EmailVerificationConfirmResult.SUCCESS)
        assertThat(stringValue(verifiedKey(TOKEN_HASH))).isEqualTo("AVAILABLE:$EMAIL_HASH")
        assertThat(stringRedisTemplate.hasKey(codeKey())).isFalse()
        assertThat(stringRedisTemplate.hasKey(attemptKey())).isFalse()
    }

    @Test
    @DisplayName("잘못된 인증번호 입력 시 실패하고 시도 횟수가 증가한다")
    fun confirm_invalidCode() {
        saveChallenge()

        val result = repository.confirm(
            EMAIL_HASH,
            TokenHashUtil.sha256("wrong-code"),
            TOKEN_HASH,
            MAX_ATTEMPTS,
            VERIFICATION_TTL,
        )

        assertThat(result).isEqualTo(EmailVerificationConfirmResult.INVALID)
        assertThat(stringValue(attemptKey())).isEqualTo("1")
        assertThat(stringRedisTemplate.hasKey(verifiedKey(TOKEN_HASH))).isFalse()
    }

    @Test
    @DisplayName("최대 시도 횟수를 초과하면 TOO_MANY_ATTEMPTS를 반환하고 코드 및 시도 횟수가 초기화된다")
    fun confirm_exceedMaxAttempts() {
        saveChallenge()

        val wrongCodeHash = TokenHashUtil.sha256("wrong-code")
        repeat(MAX_ATTEMPTS.toInt()) {
            repository.confirm(
                EMAIL_HASH,
                wrongCodeHash,
                TOKEN_HASH,
                MAX_ATTEMPTS,
                VERIFICATION_TTL,
            )
        }

        val result = repository.confirm(
            EMAIL_HASH,
            wrongCodeHash,
            TOKEN_HASH,
            MAX_ATTEMPTS,
            VERIFICATION_TTL,
        )

        assertThat(result).isEqualTo(EmailVerificationConfirmResult.TOO_MANY_ATTEMPTS)
        assertThat(stringRedisTemplate.hasKey(codeKey())).isFalse()
        assertThat(stringRedisTemplate.hasKey(attemptKey())).isFalse()
    }

    @Test
    @DisplayName("동시 요청 시 쿨다운 키로 인해 단 1개의 요청만 성공한다")
    fun saveChallenge_concurrency() {
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)
        val latch = CountDownLatch(CONCURRENT_REQUESTS)
        val results = java.util.Collections.synchronizedList(mutableListOf<Boolean>())

        repeat(CONCURRENT_REQUESTS) {
            executor.submit {
                try {
                    results.add(saveChallenge())
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertThat(results.count { it }).isEqualTo(1)
        assertThat(results.count { !it }).isEqualTo(CONCURRENT_REQUESTS - 1)
    }

    @Test
    @DisplayName("검증 상태 선점(reserve), 완료(complete), 원복(restore) 트랜잭션 정상 동작")
    fun stateTransitions() {
        saveChallenge()
        repository.confirm(EMAIL_HASH, CODE_HASH, TOKEN_HASH, MAX_ATTEMPTS, VERIFICATION_TTL)

        val reservationId = "reservation-123"

        val reserved = repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, reservationId)
        assertThat(reserved).isTrue()
        assertThat(stringValue(verifiedKey(TOKEN_HASH))).isEqualTo("RESERVED:$reservationId:$EMAIL_HASH")

        val restored = repository.restoreVerification(EMAIL_HASH, TOKEN_HASH, reservationId)
        assertThat(restored).isTrue()
        assertThat(stringValue(verifiedKey(TOKEN_HASH))).isEqualTo("AVAILABLE:$EMAIL_HASH")

        repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, reservationId)
        val completed = repository.completeVerification(EMAIL_HASH, TOKEN_HASH, reservationId)
        assertThat(completed).isTrue()
        assertThat(stringRedisTemplate.hasKey(verifiedKey(TOKEN_HASH))).isFalse()
    }

    @Test
    @DisplayName("clearChallenge 호출 시 챌린지 및 쿨다운 키가 삭제된다")
    fun clearChallenge() {
        saveChallenge()

        repository.clearChallenge(EMAIL_HASH)

        assertThat(stringValue(codeKey())).isNull()
        assertThat(stringValue(cooldownKey())).isNull()
        assertThat(stringRedisTemplate.hasKey(attemptKey())).isFalse()
    }

    private fun saveChallenge(): Boolean =
        repository.saveChallenge(
            EMAIL_HASH,
            CODE_HASH,
            CODE_TTL,
            COOLDOWN_TTL,
        )

    private fun stringValue(key: String): String? =
        stringRedisTemplate.opsForValue().get(key)

    private fun remainingTime(key: String): Long =
        stringRedisTemplate.getExpire(key, java.util.concurrent.TimeUnit.MILLISECONDS) ?: -1L

    private fun codeKey(): String = "${PREFIX}code:$EMAIL_HASH"

    private fun attemptKey(): String = "${PREFIX}attempt:$EMAIL_HASH"

    private fun cooldownKey(): String = "${PREFIX}cooldown:$EMAIL_HASH"

    private fun verifiedKey(tokenHash: String): String = "${PREFIX}verified:$tokenHash"

    companion object {
        private const val PREFIX = "test:auth:email-verification:"
        private const val EMAIL = "test@example.com"
        private val EMAIL_HASH = TokenHashUtil.sha256(EMAIL)
        private const val CODE = "123456"
        private val CODE_HASH = TokenHashUtil.sha256("$EMAIL:$CODE")
        private const val TOKEN = "verification-token"
        private val TOKEN_HASH = TokenHashUtil.sha256(TOKEN)
        private const val MAX_ATTEMPTS = 5L
        private const val CONCURRENT_REQUESTS = 10
        private val CODE_TTL = Duration.ofMinutes(5)
        private val COOLDOWN_TTL = Duration.ofMinutes(1)
        private val VERIFICATION_TTL = Duration.ofMinutes(30)
    }
}
