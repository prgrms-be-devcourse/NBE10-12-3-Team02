package com.back.domain.auth.service

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.security.email.EmailVerificationRepository
import com.back.global.security.jwt.TokenHashUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import java.time.Duration

class EmailVerificationServiceTest {
    private val mailSender = mock(JavaMailSender::class.java)
    private val repository = mock(EmailVerificationRepository::class.java)
    private val service = EmailVerificationService(
        mailSender = mailSender,
        emailVerificationRepository = repository,
        codeExpirationSeconds = CODE_EXPIRATION_SECONDS,
        verifiedExpirationSeconds = VERIFIED_EXPIRATION_SECONDS,
        resendCooldownSeconds = RESEND_COOLDOWN_SECONDS,
        maxAttempts = MAX_ATTEMPTS,
        senderEmail = SENDER_EMAIL,
    )

    @Test
    @DisplayName("인증번호를 저장하고 이메일을 발송한다")
    fun t1() {
        `when`(
            repository.saveChallenge(
                eqValue(EMAIL_HASH),
                anyValue(),
                eqValue(Duration.ofSeconds(CODE_EXPIRATION_SECONDS)),
                eqValue(Duration.ofSeconds(RESEND_COOLDOWN_SECONDS)),
            ),
        ).thenReturn(true)

        service.sendVerificationCode(" TEST@example.com ")

        verify(mailSender).send(anyValue<SimpleMailMessage>())
    }

    @Test
    @DisplayName("재전송 대기 중이면 발송하지 않는다")
    fun t2() {
        `when`(
            repository.saveChallenge(
                eqValue(EMAIL_HASH),
                anyValue(),
                anyValue(),
                anyValue(),
            ),
        ).thenReturn(false)

        assertThatThrownBy { service.sendVerificationCode(EMAIL) }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.AUTH_EMAIL_VERIFICATION_RESEND_NOT_ALLOWED)
            }
    }

    @Test
    @DisplayName("메일 발송 실패 시 인증정보와 재전송 제한을 삭제한다")
    fun t3() {
        `when`(
            repository.saveChallenge(
                eqValue(EMAIL_HASH),
                anyValue(),
                anyValue(),
                anyValue(),
            ),
        ).thenReturn(true)
        doThrow(MailSendException("send failed"))
            .`when`(mailSender)
            .send(anyValue<SimpleMailMessage>())

        assertThatThrownBy { service.sendVerificationCode(EMAIL) }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.AUTH_EMAIL_SEND_FAILED)
            }

        verify(repository).clearChallenge(EMAIL_HASH, includeCooldown = true)
    }

    @Test
    @DisplayName("올바른 인증번호면 일회용 인증 토큰을 발급한다")
    fun t4() {
        `when`(repository.getCodeHash(EMAIL_HASH)).thenReturn(TokenHashUtil.sha256("$EMAIL:$CODE"))
        `when`(repository.incrementAttempts(EMAIL_HASH, Duration.ofSeconds(CODE_EXPIRATION_SECONDS)))
            .thenReturn(1)

        val verificationToken = service.confirm(EMAIL, CODE)

        assertThat(verificationToken).isNotBlank()
        verify(repository).saveVerification(
            eqValue(EMAIL_HASH),
            anyValue(),
            eqValue(Duration.ofSeconds(VERIFIED_EXPIRATION_SECONDS)),
        )
        verify(repository).clearChallenge(EMAIL_HASH)
    }

    @Test
    @DisplayName("인증 시도 횟수를 초과하면 인증정보를 삭제한다")
    fun t5() {
        `when`(repository.getCodeHash(EMAIL_HASH)).thenReturn("saved-code-hash")
        `when`(repository.incrementAttempts(EMAIL_HASH, Duration.ofSeconds(CODE_EXPIRATION_SECONDS)))
            .thenReturn(MAX_ATTEMPTS + 1)

        assertThatThrownBy { service.confirm(EMAIL, CODE) }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.AUTH_EMAIL_VERIFICATION_TOO_MANY_ATTEMPTS)
            }

        verify(repository).clearChallenge(EMAIL_HASH)
    }

    companion object {
        private const val EMAIL = "test@example.com"
        private const val SENDER_EMAIL = "sender@naver.com"
        private val EMAIL_HASH = TokenHashUtil.sha256(EMAIL)
        private const val CODE = "123456"
        private const val CODE_EXPIRATION_SECONDS = 300L
        private const val VERIFIED_EXPIRATION_SECONDS = 1800L
        private const val RESEND_COOLDOWN_SECONDS = 60L
        private const val MAX_ATTEMPTS = 5L
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyValue(): T {
        ArgumentMatchers.any<T>()
        return null as T
    }

    private fun <T> eqValue(value: T): T {
        ArgumentMatchers.eq(value)
        return value
    }
}
