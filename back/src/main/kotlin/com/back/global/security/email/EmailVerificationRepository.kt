package com.back.global.security.email

import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class EmailVerificationRepository(
    private val redissonClient: RedissonClient,
    @Value("\${custom.auth.email-verification.redis-prefix}")
    private val prefix: String,
) {
    fun saveChallenge(
        emailHash: String,
        codeHash: String,
        codeTtl: Duration,
        resendCooldown: Duration,
    ): Boolean {
        val cooldownStarted = redissonClient
            .getBucket<String>(cooldownKey(emailHash))
            .setIfAbsent(ACTIVE_VALUE, resendCooldown)

        if (!cooldownStarted) {
            return false
        }

        redissonClient.getBucket<String>(codeKey(emailHash)).set(codeHash, codeTtl)
        redissonClient.getAtomicLong(attemptKey(emailHash)).delete()
        return true
    }

    fun getCodeHash(emailHash: String): String? =
        redissonClient.getBucket<String>(codeKey(emailHash)).get()

    fun incrementAttempts(emailHash: String, ttl: Duration): Long {
        val attempts = redissonClient.getAtomicLong(attemptKey(emailHash))
        val count = attempts.incrementAndGet()
        if (count == 1L) {
            attempts.expire(ttl)
        }
        return count
    }

    fun saveVerification(emailHash: String, tokenHash: String, ttl: Duration) {
        redissonClient.getBucket<String>(verifiedKey(tokenHash)).set(emailHash, ttl)
    }

    fun consumeVerification(emailHash: String, tokenHash: String): Boolean =
        redissonClient.getBucket<String>(verifiedKey(tokenHash)).getAndDelete() == emailHash

    fun clearChallenge(emailHash: String, includeCooldown: Boolean = false) {
        redissonClient.getBucket<String>(codeKey(emailHash)).delete()
        redissonClient.getAtomicLong(attemptKey(emailHash)).delete()

        if (includeCooldown) {
            redissonClient.getBucket<String>(cooldownKey(emailHash)).delete()
        }
    }

    private fun codeKey(emailHash: String): String = "${prefix}code:$emailHash"

    private fun attemptKey(emailHash: String): String = "${prefix}attempt:$emailHash"

    private fun cooldownKey(emailHash: String): String = "${prefix}cooldown:$emailHash"

    private fun verifiedKey(tokenHash: String): String = "${prefix}verified:$tokenHash"

    companion object {
        private const val ACTIVE_VALUE = "1"
    }
}
