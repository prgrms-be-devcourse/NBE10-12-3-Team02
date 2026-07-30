package com.back.domain.user.service

import com.back.domain.auth.entity.UserSocialAuth
import com.back.domain.auth.repository.UserSocialAuthRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class UserWithdrawalQueryServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val userSocialAuthRepository = mock(UserSocialAuthRepository::class.java)
    private val service = UserWithdrawalQueryService(
        userRepository,
        userSocialAuthRepository,
    )

    @Test
    @DisplayName("탈퇴 회원과 소셜 인증 정보를 스냅샷으로 조회한다")
    fun t1() {
        val user = user()
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(user)
        `when`(userSocialAuthRepository.findByUserUserId(USER_ID)).thenReturn(
            UserSocialAuth(
                user = user,
                provider = LoginType.KAKAO,
                providerId = PROVIDER_ID,
                oauthRefreshToken = OAUTH_REFRESH_TOKEN,
            ),
        )

        val result = service.getWithdrawalTarget(USER_ID)

        assertThat(result.userId).isEqualTo(USER_ID)
        assertThat(result.provider).isEqualTo(LoginType.KAKAO)
        assertThat(result.providerId).isEqualTo(PROVIDER_ID)
        assertThat(result.oauthRefreshToken).isEqualTo(OAUTH_REFRESH_TOKEN)
    }

    @Test
    @DisplayName("활성 회원이 없으면 탈퇴 대상 조회를 거절한다")
    fun t2() {
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(null)

        assertThatThrownBy { service.getWithdrawalTarget(USER_ID) }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.USER_NOT_FOUND_OR_DELETED)
            }
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
        private const val PROVIDER_ID = "provider-id"
        private const val OAUTH_REFRESH_TOKEN = "oauth-refresh-token"
    }
}
