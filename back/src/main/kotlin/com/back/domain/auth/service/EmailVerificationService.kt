package com.back.domain.auth.service

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.security.email.EmailVerificationRepository
import com.back.global.security.email.constant.EmailVerificationConfirmResult
import com.back.global.security.jwt.TokenHashUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration
import java.util.Locale
import java.util.UUID

@Service
class EmailVerificationService(
    private val mailSender: JavaMailSender,
    private val emailVerificationRepository: EmailVerificationRepository,
    @Value("\${custom.auth.email-verification.code-expiration-seconds}")
    private val codeExpirationSeconds: Long,
    @Value("\${custom.auth.email-verification.verified-expiration-seconds}")
    private val verifiedExpirationSeconds: Long,
    @Value("\${custom.auth.email-verification.resend-cooldown-seconds}")
    private val resendCooldownSeconds: Long,
    @Value("\${custom.auth.email-verification.max-attempts}")
    private val maxAttempts: Long,
    @Value("\${spring.mail.username}")
    private val senderEmail: String,
) {
    fun sendVerificationCode(email: String) {
        val normalizedEmail = normalizeEmail(email)
        val emailHash = TokenHashUtil.sha256(normalizedEmail)
        val code = generateCode()
        val challengeSaved = emailVerificationRepository.saveChallenge(
            emailHash = emailHash,
            codeHash = hashCode(normalizedEmail, code),
            codeTtl = codeTtl(),
            resendCooldown = Duration.ofSeconds(resendCooldownSeconds),
        )

        if (!challengeSaved) {
            throw ServiceException(ErrorCode.AUTH_EMAIL_VERIFICATION_RESEND_NOT_ALLOWED)
        }

        try {
            mailSender.send(createMessage(normalizedEmail, code))
        } catch (e: RuntimeException) {
            emailVerificationRepository.clearChallenge(emailHash, includeCooldown = true)
            throw ServiceException(ErrorCode.AUTH_EMAIL_SEND_FAILED)
        }
    }

    fun confirm(email: String, code: String): String {
        val normalizedEmail = normalizeEmail(email)
        val emailHash = TokenHashUtil.sha256(normalizedEmail)
        val verificationToken = UUID.randomUUID().toString()
        val result = emailVerificationRepository.confirm(
            emailHash = emailHash,
            requestCodeHash = hashCode(normalizedEmail, code),
            verificationTokenHash = TokenHashUtil.sha256(verificationToken),
            maxAttempts = maxAttempts,
            verificationTtl = Duration.ofSeconds(verifiedExpirationSeconds),
        )

        return when (result) {
            EmailVerificationConfirmResult.SUCCESS -> verificationToken
            EmailVerificationConfirmResult.INVALID ->
                throw ServiceException(ErrorCode.AUTH_EMAIL_VERIFICATION_INVALID)
            EmailVerificationConfirmResult.TOO_MANY_ATTEMPTS ->
                throw ServiceException(ErrorCode.AUTH_EMAIL_VERIFICATION_TOO_MANY_ATTEMPTS)
        }
    }

    fun reserveVerification(email: String, verificationToken: String): String? {
        val reservationId = UUID.randomUUID().toString()
        val reserved = emailVerificationRepository.reserveVerification(
            emailHash = emailHash(email),
            tokenHash = TokenHashUtil.sha256(verificationToken),
            reservationId = reservationId,
        )
        return reservationId.takeIf { reserved }
    }

    fun completeVerification(email: String, verificationToken: String, reservationId: String): Boolean =
        emailVerificationRepository.completeVerification(
            emailHash = emailHash(email),
            tokenHash = TokenHashUtil.sha256(verificationToken),
            reservationId = reservationId,
        )

    fun restoreVerification(email: String, verificationToken: String, reservationId: String): Boolean =
        emailVerificationRepository.restoreVerification(
            emailHash = emailHash(email),
            tokenHash = TokenHashUtil.sha256(verificationToken),
            reservationId = reservationId,
        )

    private fun createMessage(email: String, code: String): SimpleMailMessage =
        SimpleMailMessage().apply {
            from = senderEmail
            setTo(email)
            subject = "[Ticketing Go] 이메일 인증번호"
            text = """
                회원가입 이메일 인증번호는 [$code]입니다.
                인증번호는 ${codeExpirationSeconds / 60}분 동안 유효합니다.
            """.trimIndent()
        }

    private fun generateCode(): String =
        secureRandom.nextInt(CODE_BOUND).toString().padStart(CODE_LENGTH, '0')

    private fun hashCode(email: String, code: String): String =
        TokenHashUtil.sha256("$email:$code")

    private fun normalizeEmail(email: String): String =
        email.trim().lowercase(Locale.ROOT)

    private fun emailHash(email: String): String =
        TokenHashUtil.sha256(normalizeEmail(email))

    private fun codeTtl(): Duration = Duration.ofSeconds(codeExpirationSeconds)

    companion object {
        private const val CODE_LENGTH = 6
        private const val CODE_BOUND = 1_000_000
        private val secureRandom = SecureRandom()
    }
}
