package com.back.domain.auth.service

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

class SocialLinkQueryServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val userSocialAuthRepository = mock(UserSocialAuthRepository::class.java)
    private val service = SocialLinkQueryService(userRepository, userSocialAuthRepository)

    @Test
    @DisplayName("일반 회원의 현재 소셜 연동 정보를 해제 대상 스냅샷으로 반환한다")
    fun t1() {
        val user = normalUser()
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(user)
        `when`(userSocialAuthRepository.findByUserUserId(USER_ID)).thenReturn(
            UserSocialAuth(user, LoginType.NAVER, PROVIDER_ID, OAUTH_REFRESH_TOKEN),
        )

        val result = service.getUnlinkTarget(USER_ID)

        assertThat(result.userId).isEqualTo(USER_ID)
        assertThat(result.provider).isEqualTo(LoginType.NAVER)
        assertThat(result.providerId).isEqualTo(PROVIDER_ID)
        assertThat(result.oauthRefreshToken).isEqualTo(OAUTH_REFRESH_TOKEN)
    }

    @Test
    @DisplayName("소셜 로그인으로 가입한 회원은 유일한 로그인 수단을 해제할 수 없다")
    fun t2() {
        val user = User.create(
            loginId = "NAVER_$PROVIDER_ID",
            email = EMAIL,
            password = "random-encoded-password",
            name = "네이버회원",
            loginType = LoginType.NAVER,
        )
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(user)

        assertThatThrownBy { service.getUnlinkTarget(USER_ID) }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.OAUTH_UNLINK_NOT_ALLOWED)
            }
    }

    @Test
    @DisplayName("연결된 소셜 계정이 없는 회원은 연동을 해제할 수 없다")
    fun t3() {
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(normalUser())

        assertThatThrownBy { service.getUnlinkTarget(USER_ID) }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.OAUTH_ACCOUNT_NOT_LINKED)
            }
    }

    private fun normalUser(): User =
        User.create(
            loginId = "normal-user",
            email = EMAIL,
            password = "encoded-password",
            name = "일반회원",
            loginType = LoginType.NORMAL,
        )

    companion object {
        private const val USER_ID = 1L
        private const val PROVIDER_ID = "provider-id"
        private const val EMAIL = "user@example.com"
        private const val OAUTH_REFRESH_TOKEN = "oauth-refresh-token"
    }
}
