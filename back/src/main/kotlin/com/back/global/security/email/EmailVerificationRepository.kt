package com.back.global.security.email

import com.back.global.security.email.constant.EmailVerificationConfirmResult
import org.redisson.api.RBucket
import org.redisson.api.RedissonClient
import org.redisson.api.RScript
import org.redisson.client.codec.StringCodec
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
        val cooldownStarted = stringBucket(cooldownKey(emailHash))
            .setIfAbsent(ACTIVE_VALUE, resendCooldown)

        if (!cooldownStarted) {
            return false
        }

        stringBucket(codeKey(emailHash)).set(codeHash, codeTtl)
        redissonClient.getAtomicLong(attemptKey(emailHash)).delete()
        return true
    }

    fun confirm(
        emailHash: String,
        requestCodeHash: String,
        verificationTokenHash: String,
        maxAttempts: Long,
        verificationTtl: Duration,
    ): EmailVerificationConfirmResult {
        val result = redissonClient.getScript(StringCodec.INSTANCE).eval<Long>(
            RScript.Mode.READ_WRITE,
            EmailVerificationLuaScripts.confirmScript(),
            RScript.ReturnType.LONG,
            listOf(
                codeKey(emailHash),
                attemptKey(emailHash),
                verifiedKey(verificationTokenHash),
            ),
            requestCodeHash,
            maxAttempts.toString(),
            emailHash,
            verificationTtl.toMillis().toString(),
        )

        return when (result?.toInt()) {
            1 -> EmailVerificationConfirmResult.SUCCESS
            -2 -> EmailVerificationConfirmResult.TOO_MANY_ATTEMPTS
            -1, 0 -> EmailVerificationConfirmResult.INVALID
            else -> throw IllegalStateException("Unexpected email verification result: $result")
        }
    }

    fun consumeVerification(emailHash: String, tokenHash: String): Boolean =
        stringBucket(verifiedKey(tokenHash)).getAndDelete() == emailHash

    fun clearChallenge(emailHash: String, includeCooldown: Boolean = false) {
        stringBucket(codeKey(emailHash)).delete()
        redissonClient.getAtomicLong(attemptKey(emailHash)).delete()

        if (includeCooldown) {
            stringBucket(cooldownKey(emailHash)).delete()
        }
    }

    private fun stringBucket(key: String): RBucket<String> =
        redissonClient.getBucket(key, StringCodec.INSTANCE)

    private fun codeKey(emailHash: String): String = "${prefix}code:$emailHash"

    private fun attemptKey(emailHash: String): String = "${prefix}attempt:$emailHash"

    private fun cooldownKey(emailHash: String): String = "${prefix}cooldown:$emailHash"

    private fun verifiedKey(tokenHash: String): String = "${prefix}verified:$tokenHash"

    companion object {
        private const val ACTIVE_VALUE = "1"
    }
}
