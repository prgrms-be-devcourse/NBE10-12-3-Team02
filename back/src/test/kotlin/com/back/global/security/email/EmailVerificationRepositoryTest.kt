package com.back.global.security.email

import com.back.global.security.email.constant.EmailVerificationConfirmResult
import com.back.global.security.jwt.TokenHashUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailVerificationRepositoryTest {
    private lateinit var connectionFactory: LettuceConnectionFactory
    private lateinit var stringRedisTemplate: StringRedisTemplate
    private lateinit var repository: EmailVerificationRepository

    @BeforeAll
    fun setUpClient() {
        connectionFactory = LettuceConnectionFactory(redis.host, redis.getMappedPort(REDIS_PORT)).apply {
            afterPropertiesSet()
        }
        stringRedisTemplate = StringRedisTemplate(connectionFactory)
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
    }

    @BeforeEach
    fun clearRedis() {
        val keys = stringRedisTemplate.keys("$PREFIX*")
        if (!keys.isNullOrEmpty()) {
            stringRedisTemplate.delete(keys)
        }
    }

    @AfterAll
    fun tearDownClient() {
        connectionFactory.destroy()
    }

    @Test
    @DisplayName("최초 챌린지 생성 시 코드, 시도 횟수, 쿨다운 키가 생성된다")
    fun t1() {
        val result = saveChallenge()

        assertThat(result).isTrue()
        assertThat(stringValue(codeKey())).isEqualTo(CODE_HASH)
        assertThat(remainingTime(codeKey())).isPositive()
        assertThat(stringValue(cooldownKey())).isEqualTo("1")
        assertThat(remainingTime(cooldownKey())).isPositive()
        assertThat(stringRedisTemplate.hasKey(attemptKey())).isFalse()
    }

    @Test
    @DisplayName("올바른 인증번호 검증 성공 시 챌린지 데이터가 삭제되고 검증 완료 상태가 등록된다")
    fun t2() {
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
        assertThat(stringValue(codeKey())).isNull()
        assertThat(stringRedisTemplate.hasKey(attemptKey())).isFalse()
    }

    @Test
    @DisplayName("잘못된 인증번호는 시도 횟수와 TTL을 증가시킨다")
    fun t3() {
        saveChallenge()

        val result = repository.confirm(
            EMAIL_HASH,
            TokenHashUtil.sha256("$EMAIL:000000"),
            TOKEN_HASH,
            MAX_ATTEMPTS,
            VERIFICATION_TTL,
        )

        assertThat(result).isEqualTo(EmailVerificationConfirmResult.INVALID)
        assertThat(stringValue(attemptKey())?.toLong()).isEqualTo(1L)
        assertThat(remainingTime(attemptKey())).isPositive()
    }

    @Test
    @DisplayName("최대 시도 횟수 초과 시 인증번호와 시도 횟수를 삭제한다")
    fun t4() {
        saveChallenge()
        repeat(MAX_ATTEMPTS.toInt()) {
            repository.confirm(
                EMAIL_HASH,
                TokenHashUtil.sha256("$EMAIL:000000"),
                TOKEN_HASH,
                MAX_ATTEMPTS,
                VERIFICATION_TTL,
            )
        }

        val result = repository.confirm(
            EMAIL_HASH,
            CODE_HASH,
            TOKEN_HASH,
            MAX_ATTEMPTS,
            VERIFICATION_TTL,
        )

        assertThat(result).isEqualTo(EmailVerificationConfirmResult.TOO_MANY_ATTEMPTS)
        assertThat(stringValue(codeKey())).isNull()
        assertThat(stringRedisTemplate.hasKey(attemptKey())).isFalse()
    }

    @Test
    @DisplayName("동시 요청 시 최대 1번만 검증에 성공해야 한다")
    fun t5() {
        saveChallenge()
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)
        val readyLatch = CountDownLatch(CONCURRENT_REQUESTS)
        val startLatch = CountDownLatch(1)

        val results = List(CONCURRENT_REQUESTS) { index ->
            executor.submit<EmailVerificationConfirmResult> {
                readyLatch.countDown()
                startLatch.await()
                repository.confirm(
                    EMAIL_HASH,
                    CODE_HASH,
                    TokenHashUtil.sha256("$TOKEN-$index"),
                    MAX_ATTEMPTS,
                    VERIFICATION_TTL,
                )
            }
        }

        readyLatch.await()
        startLatch.countDown()

        val confirmResults = results.map { it.get() }
        executor.shutdown()

        assertThat(confirmResults.count { it == EmailVerificationConfirmResult.SUCCESS }).isEqualTo(1)
        assertThat(stringValue(codeKey())).isNull()
    }

    @Test
    @DisplayName("예약/완료/복구 플로우가 정상적으로 검증 전이된다")
    fun t6() {
        saveChallenge()
        repository.confirm(
            EMAIL_HASH,
            CODE_HASH,
            TOKEN_HASH,
            MAX_ATTEMPTS,
            VERIFICATION_TTL,
        )

        val reservationId = "res-123"
        assertThat(repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, reservationId)).isTrue()
        assertThat(stringValue(verifiedKey(TOKEN_HASH))).isEqualTo("RESERVED:$reservationId:$EMAIL_HASH")

        assertThat(repository.restoreVerification(EMAIL_HASH, TOKEN_HASH, reservationId)).isTrue()
        assertThat(stringValue(verifiedKey(TOKEN_HASH))).isEqualTo("AVAILABLE:$EMAIL_HASH")

        assertThat(repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, reservationId)).isTrue()
        assertThat(repository.completeVerification(EMAIL_HASH, TOKEN_HASH, reservationId)).isTrue()
        assertThat(stringValue(verifiedKey(TOKEN_HASH))).isNull()
    }

    @Test
    @DisplayName("clearChallenge 호출 시 챌린지 관련 키가 전부 제거된다")
    fun t7() {
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
        private const val REDIS_PORT = 6379
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

        @Container
        @JvmStatic
        private val redis = RedisContainer(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(REDIS_PORT)
    }
}

private class RedisContainer(imageName: DockerImageName) :
    GenericContainer<RedisContainer>(imageName)
