package com.back.domain.auth.service

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.security.email.EmailVerificationProperties
import com.back.global.security.email.EmailVerificationRepository
import com.back.global.security.email.constant.EmailVerificationConfirmResult
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
        properties = EmailVerificationProperties(
            codeExpirationSeconds = CODE_EXPIRATION_SECONDS,
            verifiedExpirationSeconds = VERIFIED_EXPIRATION_SECONDS,
            resendCooldownSeconds = RESEND_COOLDOWN_SECONDS,
            maxAttempts = MAX_ATTEMPTS,
            redisPrefix = REDIS_PREFIX,
        ),
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
    @DisplayName("재전송 대기 중이면 인증번호를 발송하지 않는다")
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
        `when`(
            repository.confirm(
                eqValue(EMAIL_HASH),
                eqValue(TokenHashUtil.sha256("$EMAIL:$CODE")),
                anyValue(),
                eqValue(MAX_ATTEMPTS),
                eqValue(Duration.ofSeconds(VERIFIED_EXPIRATION_SECONDS)),
            ),
        ).thenReturn(EmailVerificationConfirmResult.SUCCESS)

        val verificationToken = service.confirm(EMAIL, CODE)

        assertThat(verificationToken).isNotBlank()
    }

    @Test
    @DisplayName("인증 시도 횟수를 초과하면 인증정보를 삭제한다")
    fun t5() {
        `when`(
            repository.confirm(
                eqValue(EMAIL_HASH),
                anyValue(),
                anyValue(),
                eqValue(MAX_ATTEMPTS),
                anyValue(),
            ),
        ).thenReturn(EmailVerificationConfirmResult.TOO_MANY_ATTEMPTS)

        assertThatThrownBy { service.confirm(EMAIL, CODE) }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.AUTH_EMAIL_VERIFICATION_TOO_MANY_ATTEMPTS)
            }
    }

    @Test
    @DisplayName("유효한 인증 토큰을 회원가입에 사용하도록 예약한다")
    fun t6() {
        `when`(
            repository.reserveVerification(
                eqValue(EMAIL_HASH),
                eqValue(TokenHashUtil.sha256(VERIFICATION_TOKEN)),
                anyValue(),
            ),
        ).thenReturn(true)

        val reservationId = service.reserveVerification(EMAIL, VERIFICATION_TOKEN)

        assertThat(reservationId).isNotBlank()
    }

    @Test
    @DisplayName("유효하지 않거나 이미 예약된 인증 토큰은 예약하지 않는다")
    fun t7() {
        `when`(
            repository.reserveVerification(
                eqValue(EMAIL_HASH),
                eqValue(TokenHashUtil.sha256(VERIFICATION_TOKEN)),
                anyValue(),
            ),
        ).thenReturn(false)

        val reservationId = service.reserveVerification(EMAIL, VERIFICATION_TOKEN)

        assertThat(reservationId).isNull()
    }

    @Test
    @DisplayName("회원가입 커밋 성공 시 예약된 인증 토큰을 최종 소비한다")
    fun t8() {
        `when`(
            repository.completeVerification(
                EMAIL_HASH,
                TokenHashUtil.sha256(VERIFICATION_TOKEN),
                RESERVATION_ID,
            ),
        ).thenReturn(true)

        val completed = service.completeVerification(EMAIL, VERIFICATION_TOKEN, RESERVATION_ID)

        assertThat(completed).isTrue()
        verify(repository).completeVerification(
            EMAIL_HASH,
            TokenHashUtil.sha256(VERIFICATION_TOKEN),
            RESERVATION_ID,
        )
    }

    @Test
    @DisplayName("회원가입 롤백 시 예약된 인증 토큰을 다시 사용 가능한 상태로 복구한다")
    fun t9() {
        `when`(
            repository.restoreVerification(
                EMAIL_HASH,
                TokenHashUtil.sha256(VERIFICATION_TOKEN),
                RESERVATION_ID,
            ),
        ).thenReturn(true)

        val restored = service.restoreVerification(EMAIL, VERIFICATION_TOKEN, RESERVATION_ID)

        assertThat(restored).isTrue()
        verify(repository).restoreVerification(
            EMAIL_HASH,
            TokenHashUtil.sha256(VERIFICATION_TOKEN),
            RESERVATION_ID,
        )
    }

    @Test
    @DisplayName("메일 발송과 인증정보 정리가 모두 실패해도 이메일 전송 실패 예외를 반환한다")
    fun t10() {
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
        doThrow(IllegalStateException("cleanup failed"))
            .`when`(repository)
            .clearChallenge(EMAIL_HASH, includeCooldown = true)

        assertThatThrownBy { service.sendVerificationCode(EMAIL) }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.AUTH_EMAIL_SEND_FAILED)
            }
    }

    companion object {
        private const val EMAIL = "test@example.com"
        private const val SENDER_EMAIL = "sender@naver.com"
        private const val REDIS_PREFIX = "auth:email-verification:"
        private const val VERIFICATION_TOKEN = "verification-token"
        private const val RESERVATION_ID = "reservation-id"
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
