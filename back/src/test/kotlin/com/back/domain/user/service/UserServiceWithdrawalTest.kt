package com.back.domain.user.service

import com.back.domain.auth.repository.UserSocialAuthRepository
import com.back.domain.auth.service.EmailVerificationService
import com.back.domain.ticket.repository.TicketRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.dto.UserWithdrawalTarget
import com.back.domain.user.repository.UserRepository
import com.back.global.file.FileStorage
import com.back.global.security.filter.BearerTokenExtractor
import com.back.global.security.oauth2.service.OAuthUnlinkResult
import com.back.global.security.oauth2.service.OAuthUnlinkService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceWithdrawalTest {
    private val userWithdrawalQueryService = mock(UserWithdrawalQueryService::class.java)
    private val userWithdrawalCommandService = mock(UserWithdrawalCommandService::class.java)
    private val bearerTokenExtractor = mock(BearerTokenExtractor::class.java)
    private val oAuthUnlinkService = mock(OAuthUnlinkService::class.java)
    private val service = UserService(
        userRepository = mock(UserRepository::class.java),
        userSocialAuthRepository = mock(UserSocialAuthRepository::class.java),
        ticketRepository = mock(TicketRepository::class.java),
        passwordEncoder = mock(PasswordEncoder::class.java),
        emailVerificationService = mock(EmailVerificationService::class.java),
        bearerTokenExtractor = bearerTokenExtractor,
        oAuthUnlinkService = oAuthUnlinkService,
        userWithdrawalQueryService = userWithdrawalQueryService,
        userWithdrawalCommandService = userWithdrawalCommandService,
        fileStorage = mock(FileStorage::class.java),
    )

    @Test
    @DisplayName("Provider 연결 해제가 실패해도 내부 회원 탈퇴를 계속한다")
    fun t1() {
        val target = UserWithdrawalTarget(
            userId = USER_ID,
            provider = LoginType.NAVER,
            providerId = "provider-id",
            oauthRefreshToken = "oauth-refresh-token",
        )
        `when`(bearerTokenExtractor.extract(AUTHORIZATION)).thenReturn(ACCESS_TOKEN)
        `when`(userWithdrawalQueryService.getWithdrawalTarget(USER_ID)).thenReturn(target)
        `when`(oAuthUnlinkService.unlink(LoginType.NAVER, "oauth-refresh-token"))
            .thenReturn(OAuthUnlinkResult.FAILED)

        service.withdraw(USER_ID, AUTHORIZATION)

        verify(userWithdrawalCommandService).withdraw(USER_ID, ACCESS_TOKEN)
    }

    companion object {
        private const val USER_ID = 1L
        private const val AUTHORIZATION = "Bearer access-token"
        private const val ACCESS_TOKEN = "access-token"
    }
}
