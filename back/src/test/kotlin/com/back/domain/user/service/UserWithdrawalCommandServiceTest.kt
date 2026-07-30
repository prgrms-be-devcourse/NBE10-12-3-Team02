package com.back.domain.user.service

import com.back.domain.auth.repository.UserSocialAuthRepository
import com.back.domain.ticket.repository.TicketRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.file.FileStorage
import com.back.global.security.jwt.JwtTokenProvider
import com.back.global.security.jwt.repository.BlacklistRepository
import com.back.global.security.jwt.repository.RefreshTokenRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.time.Duration

class UserWithdrawalCommandServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val userSocialAuthRepository = mock(UserSocialAuthRepository::class.java)
    private val ticketRepository = mock(TicketRepository::class.java)
    private val refreshTokenRepository = mock(RefreshTokenRepository::class.java)
    private val blacklistRepository = mock(BlacklistRepository::class.java)
    private val jwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val fileStorage = mock(FileStorage::class.java)
    private val service = UserWithdrawalCommandService(
        userRepository = userRepository,
        userSocialAuthRepository = userSocialAuthRepository,
        ticketRepository = ticketRepository,
        refreshTokenRepository = refreshTokenRepository,
        blacklistRepository = blacklistRepository,
        jwtTokenProvider = jwtTokenProvider,
        eventPublisher = eventPublisher,
        fileStorage = fileStorage,
        tokenBlacklistGraceSeconds = 60,
    )

    @Test
    @DisplayName("내부 탈퇴 트랜잭션에서 소셜 인증과 토큰을 제거하고 회원을 소프트 삭제한다")
    fun t1() {
        val user = user()
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(user)
        `when`(ticketRepository.findAllByUserWithConcert(user)).thenReturn(emptyList())
        `when`(jwtTokenProvider.getRemainingSeconds(ACCESS_TOKEN)).thenReturn(600)

        service.withdraw(USER_ID, ACCESS_TOKEN)

        assertThat(user.isDeleted).isTrue()
        verify(userSocialAuthRepository).deleteByUserUserId(USER_ID)
        verify(refreshTokenRepository).deleteAllByUserId(USER_ID)
        verify(blacklistRepository).add(ACCESS_TOKEN, Duration.ofSeconds(660))
    }

    private fun user(): User =
        User.create(
            loginId = "normal-user",
            email = "user@example.com",
            password = "encoded-password",
            name = "회원",
            loginType = LoginType.NORMAL,
        )

    companion object {
        private const val USER_ID = 1L
        private const val ACCESS_TOKEN = "access-token"
    }
}
