package com.back.global.security.email

import com.back.global.security.email.constant.EmailVerificationConfirmResult
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class EmailVerificationRepository(
    private val stringRedisTemplate: StringRedisTemplate,
    private val properties: EmailVerificationProperties,
) {
    fun saveChallenge(
        emailHash: String,
        codeHash: String,
        codeTtl: Duration,
        resendCooldown: Duration,
    ): Boolean {
        val result = stringRedisTemplate.execute(
            SAVE_CHALLENGE_SCRIPT,
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
        val result = stringRedisTemplate.execute(
            CONFIRM_SCRIPT,
            listOf(
                codeKey(emailHash),
                attemptKey(emailHash),
                verifiedKey(verificationTokenHash),
            ),
            requestCodeHash,
            maxAttempts.toString(),
            availableValue(emailHash),
            verificationTtl.toMillis().toString(),
        )

        return when (result?.toInt()) {
            1 -> EmailVerificationConfirmResult.SUCCESS
            -2 -> EmailVerificationConfirmResult.TOO_MANY_ATTEMPTS
            -1, 0 -> EmailVerificationConfirmResult.INVALID
            else -> throw IllegalStateException("Unexpected email verification result: $result")
        }
    }

    fun reserveVerification(emailHash: String, tokenHash: String, reservationId: String): Boolean =
        executeStateTransition(
            script = RESERVE_SCRIPT,
            tokenHash = tokenHash,
            expectedValue = availableValue(emailHash),
            newValue = reservedValue(emailHash, reservationId),
        )

    fun completeVerification(emailHash: String, tokenHash: String, reservationId: String): Boolean =
        executeStateTransition(
            script = COMPLETE_RESERVATION_SCRIPT,
            tokenHash = tokenHash,
            expectedValue = reservedValue(emailHash, reservationId),
        )

    fun restoreVerification(emailHash: String, tokenHash: String, reservationId: String): Boolean =
        executeStateTransition(
            script = RESTORE_RESERVATION_SCRIPT,
            tokenHash = tokenHash,
            expectedValue = reservedValue(emailHash, reservationId),
            newValue = availableValue(emailHash),
        )

    fun clearChallenge(emailHash: String) {
        stringRedisTemplate.execute(
            CLEAR_CHALLENGE_SCRIPT,
            listOf(
                codeKey(emailHash),
                attemptKey(emailHash),
                cooldownKey(emailHash),
            )
        )
    }

    private fun codeKey(emailHash: String): String = "${properties.redisPrefix}code:$emailHash"

    private fun attemptKey(emailHash: String): String = "${properties.redisPrefix}attempt:$emailHash"

    private fun cooldownKey(emailHash: String): String = "${properties.redisPrefix}cooldown:$emailHash"

    private fun verifiedKey(tokenHash: String): String = "${properties.redisPrefix}verified:$tokenHash"

    private fun availableValue(emailHash: String): String = "$AVAILABLE_PREFIX$emailHash"

    private fun reservedValue(emailHash: String, reservationId: String): String =
        "$RESERVED_PREFIX$reservationId:$emailHash"

    private fun executeStateTransition(
        script: DefaultRedisScript<Long>,
        tokenHash: String,
        expectedValue: String,
        newValue: String? = null,
    ): Boolean {
        val args = if (newValue == null) {
            arrayOf(expectedValue)
        } else {
            arrayOf(expectedValue, newValue)
        }
        val result = stringRedisTemplate.execute(
            script,
            listOf(verifiedKey(tokenHash)),
            *args
        )
        return result == 1L
    }

    companion object {
        private const val ACTIVE_VALUE = "1"
        private const val AVAILABLE_PREFIX = "AVAILABLE:"
        private const val RESERVED_PREFIX = "RESERVED:"

        private val SAVE_CHALLENGE_SCRIPT = DefaultRedisScript(EmailVerificationLuaScripts.saveChallengeScript(), Long::class.java)
        private val CONFIRM_SCRIPT = DefaultRedisScript(EmailVerificationLuaScripts.confirmScript(), Long::class.java)
        private val RESERVE_SCRIPT = DefaultRedisScript(EmailVerificationLuaScripts.reserveScript(), Long::class.java)
        private val COMPLETE_RESERVATION_SCRIPT = DefaultRedisScript(EmailVerificationLuaScripts.completeReservationScript(), Long::class.java)
        private val RESTORE_RESERVATION_SCRIPT = DefaultRedisScript(EmailVerificationLuaScripts.restoreReservationScript(), Long::class.java)
        private val CLEAR_CHALLENGE_SCRIPT = DefaultRedisScript(EmailVerificationLuaScripts.clearChallengeScript(), Long::class.java)
    }
}
