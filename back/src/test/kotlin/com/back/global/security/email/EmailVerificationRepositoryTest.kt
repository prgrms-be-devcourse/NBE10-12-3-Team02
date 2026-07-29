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
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailVerificationRepositoryTest {
    private lateinit var redissonClient: RedissonClient
    private lateinit var repository: EmailVerificationRepository

    @BeforeAll
    fun setUpClient() {
        val config = Config().apply {
            codec = JsonJacksonCodec()
            useSingleServer().address = "redis://${redis.host}:${redis.getMappedPort(REDIS_PORT)}"
        }
        redissonClient = Redisson.create(config)
        repository = EmailVerificationRepository(
            redissonClient,
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
        redissonClient.keys.deleteByPattern("$PREFIX*")
    }

    @AfterAll
    fun tearDownClient() {
        redissonClient.shutdown()
    }

    @Test
    @DisplayName("인증번호 저장과 재전송 제한을 원자적으로 처리한다")
    fun t1() {
        val first = saveChallenge()
        val second = saveChallenge()

        assertThat(first).isTrue()
        assertThat(second).isFalse()
        assertThat(stringValue(codeKey())).isEqualTo(CODE_HASH)
        assertThat(remainingTime(codeKey())).isPositive()
        assertThat(remainingTime(cooldownKey())).isPositive()
    }

    @Test
    @DisplayName("올바른 인증번호 확인 시 AVAILABLE 인증 토큰을 저장한다")
    fun t2() {
        saveChallenge()

        val result = confirm(TOKEN_HASH)

        assertThat(result).isEqualTo(EmailVerificationConfirmResult.SUCCESS)
        assertThat(stringValue(verifiedKey(TOKEN_HASH))).isEqualTo("AVAILABLE:$EMAIL_HASH")
        assertThat(stringValue(codeKey())).isNull()
        assertThat(redissonClient.getAtomicLong(attemptKey()).isExists).isFalse()
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
        assertThat(redissonClient.getAtomicLong(attemptKey()).get()).isEqualTo(1)
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
            TokenHashUtil.sha256("$EMAIL:000000"),
            TOKEN_HASH,
            MAX_ATTEMPTS,
            VERIFICATION_TTL,
        )

        assertThat(result).isEqualTo(EmailVerificationConfirmResult.TOO_MANY_ATTEMPTS)
        assertThat(stringValue(codeKey())).isNull()
        assertThat(redissonClient.getAtomicLong(attemptKey()).isExists).isFalse()
    }

    @Test
    @DisplayName("동일 인증 토큰은 한 번만 예약할 수 있다")
    fun t5() {
        createVerifiedToken()

        val first = repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, "reservation-1")
        val second = repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, "reservation-2")

        assertThat(first).isTrue()
        assertThat(second).isFalse()
    }

    @Test
    @DisplayName("예약 ID가 일치할 때만 복구하고 다시 예약할 수 있다")
    fun t6() {
        createVerifiedToken()
        repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, "reservation-1")

        val wrongRestore = repository.restoreVerification(EMAIL_HASH, TOKEN_HASH, "reservation-2")
        val restored = repository.restoreVerification(EMAIL_HASH, TOKEN_HASH, "reservation-1")
        val reservedAgain = repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, "reservation-3")

        assertThat(wrongRestore).isFalse()
        assertThat(restored).isTrue()
        assertThat(reservedAgain).isTrue()
    }

    @Test
    @DisplayName("예약 완료 후 인증 토큰을 다시 사용할 수 없다")
    fun t7() {
        createVerifiedToken()
        repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, "reservation-1")

        val wrongComplete = repository.completeVerification(EMAIL_HASH, TOKEN_HASH, "reservation-2")
        val completed = repository.completeVerification(EMAIL_HASH, TOKEN_HASH, "reservation-1")
        val reservedAgain = repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, "reservation-3")

        assertThat(wrongComplete).isFalse()
        assertThat(completed).isTrue()
        assertThat(reservedAgain).isFalse()
    }

    @Test
    @DisplayName("예약과 복구 상태 전환 시 인증 토큰 TTL을 연장하지 않는다")
    fun t8() {
        createVerifiedToken()
        val initialTtl = remainingTime(verifiedKey(TOKEN_HASH))

        Thread.sleep(50)
        repository.reserveVerification(EMAIL_HASH, TOKEN_HASH, "reservation-1")
        val reservedTtl = remainingTime(verifiedKey(TOKEN_HASH))

        Thread.sleep(50)
        repository.restoreVerification(EMAIL_HASH, TOKEN_HASH, "reservation-1")
        val restoredTtl = remainingTime(verifiedKey(TOKEN_HASH))

        assertThat(reservedTtl).isPositive().isLessThan(initialTtl)
        assertThat(restoredTtl).isPositive().isLessThan(reservedTtl)
    }

    @Test
    @DisplayName("동시 예약 요청 중 하나만 성공한다")
    fun t9() {
        createVerifiedToken()
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)

        val results = try {
            executor.invokeAll(
                (1..CONCURRENT_REQUESTS).map {
                    Callable {
                        repository.reserveVerification(
                            EMAIL_HASH,
                            TOKEN_HASH,
                            UUID.randomUUID().toString(),
                        )
                    }
                },
            ).map { it.get() }
        } finally {
            executor.shutdown()
        }

        assertThat(results.count { it }).isEqualTo(1)
    }

    @Test
    @DisplayName("인증정보 정리 시 인증번호, 시도 횟수와 재전송 제한을 모두 삭제한다")
    fun t10() {
        saveChallenge()
        repository.confirm(
            EMAIL_HASH,
            TokenHashUtil.sha256("$EMAIL:000000"),
            TOKEN_HASH,
            MAX_ATTEMPTS,
            VERIFICATION_TTL,
        )

        repository.clearChallenge(EMAIL_HASH)

        assertThat(stringValue(codeKey())).isNull()
        assertThat(redissonClient.getAtomicLong(attemptKey()).isExists).isFalse()
        assertThat(stringValue(cooldownKey())).isNull()
    }

    private fun createVerifiedToken() {
        saveChallenge()
        assertThat(confirm(TOKEN_HASH)).isEqualTo(EmailVerificationConfirmResult.SUCCESS)
    }

    private fun saveChallenge(): Boolean =
        repository.saveChallenge(EMAIL_HASH, CODE_HASH, CODE_TTL, COOLDOWN_TTL)

    private fun confirm(tokenHash: String): EmailVerificationConfirmResult =
        repository.confirm(
            EMAIL_HASH,
            CODE_HASH,
            tokenHash,
            MAX_ATTEMPTS,
            VERIFICATION_TTL,
        )

    private fun stringValue(key: String): String? =
        redissonClient.getBucket<String>(key, StringCodec.INSTANCE).get()

    private fun remainingTime(key: String): Long =
        redissonClient.getBucket<String>(key, StringCodec.INSTANCE).remainTimeToLive()

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
