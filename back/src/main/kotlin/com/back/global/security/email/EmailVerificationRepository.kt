package com.back.global.security.email

import com.back.global.security.email.constant.EmailVerificationConfirmResult
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
        val result = redissonClient.getScript(StringCodec.INSTANCE).eval<Long>(
            RScript.Mode.READ_WRITE,
            EmailVerificationLuaScripts.saveChallengeScript(),
            RScript.ReturnType.LONG,
            listOf(
                cooldownKey(emailHash),
                codeKey(emailHash),
                attemptKey(emailHash),
            ),
            ACTIVE_VALUE,
            resendCooldown.toMillis().toString(),
            codeHash,
            codeTtl.toMillis().toString(),
        )

        return result == 1L
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
        redissonClient
            .getBucket<String>(verifiedKey(tokenHash), StringCodec.INSTANCE)
            .getAndDelete() == emailHash

    fun clearChallenge(emailHash: String, includeCooldown: Boolean = false) {
        redissonClient
            .getBucket<String>(codeKey(emailHash), StringCodec.INSTANCE)
            .delete()
        redissonClient.getAtomicLong(attemptKey(emailHash)).delete()

        if (includeCooldown) {
            redissonClient
                .getBucket<String>(cooldownKey(emailHash), StringCodec.INSTANCE)
                .delete()
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
