package com.back.domain.auth.service

import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.domain.auth.repository.SocialLinkIntent
import com.back.domain.auth.repository.SocialLinkIntentRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.security.oauth2.info.GoogleOAuth2UserInfo
import com.back.global.security.oauth2.service.OAuthUnlinkService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Duration

class SocialLinkServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val intentRepository = mock(SocialLinkIntentRepository::class.java)
    private val unlinkService = mock(OAuthUnlinkService::class.java)
    private val service = SocialLinkService(
        userRepository = userRepository,
        socialLinkIntentRepository = intentRepository,
        oAuthUnlinkService = unlinkService,
        linkIntentExpirationSeconds = 300,
    )

    @Test
    @DisplayName("연동 시작 시 일회성 요청을 저장하고 Provider 인증 경로를 반환한다")
    fun t1() {
        val user = normalUser()
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(user)

        val result = service.start(USER_ID, "kakao")

        assertThat(result.authorizationPath).isEqualTo("/oauth2/authorization/kakao")
        assertThat(result.intentId).isNotBlank()
        verify(intentRepository).save(
            result.intentId,
            USER_ID,
            LoginType.KAKAO,
            Duration.ofMinutes(5),
        )
    }

    @Test
    @DisplayName("이미 소셜 계정이 연결된 회원은 추가 연동을 시작할 수 없다")
    fun t2() {
        val user = normalUser().apply {
            linkSocialAccount(LoginType.KAKAO, "kakao-id", null)
        }
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(user)

        assertThatThrownBy { service.start(USER_ID, "google") }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED)
            }
    }

    @Test
    @DisplayName("인증된 Provider 이메일이 기존 이메일과 일치하면 소셜 계정을 연결한다")
    fun t3() {
        val user = normalUser()
        val userInfo = googleUserInfo(EMAIL)
        `when`(intentRepository.consume(INTENT_ID))
            .thenReturn(SocialLinkIntent(USER_ID, LoginType.GOOGLE))
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(user)
        `when`(
            userRepository.existsBySocialProviderAndSocialProviderIdAndDeletedAtIsNull(
                LoginType.GOOGLE,
                PROVIDER_ID,
            ),
        ).thenReturn(false)
        `when`(userRepository.saveAndFlush(user)).thenReturn(user)

        val result = service.complete(
            intentId = INTENT_ID,
            provider = LoginType.GOOGLE,
            userInfo = userInfo,
            oauthRefreshToken = OAUTH_REFRESH_TOKEN,
        )

        assertThat(result).isSameAs(user)
        assertThat(user.loginType).isEqualTo(LoginType.NORMAL)
        assertThat(user.socialProvider).isEqualTo(LoginType.GOOGLE)
        assertThat(user.socialProviderId).isEqualTo(PROVIDER_ID)
        assertThat(user.oauthRefreshToken).isEqualTo(OAUTH_REFRESH_TOKEN)
    }

    @Test
    @DisplayName("Provider 이메일이 기존 회원 이메일과 다르면 연동을 거절한다")
    fun t4() {
        `when`(intentRepository.consume(INTENT_ID))
            .thenReturn(SocialLinkIntent(USER_ID, LoginType.GOOGLE))
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(normalUser())

        assertOAuth2Error("oauth2_email_mismatch") {
            service.complete(
                intentId = INTENT_ID,
                provider = LoginType.GOOGLE,
                userInfo = googleUserInfo("other@example.com"),
                oauthRefreshToken = null,
            )
        }
    }

    @Test
    @DisplayName("Provider에서 인증되지 않은 이메일이면 연동을 거절한다")
    fun t5() {
        `when`(intentRepository.consume(INTENT_ID))
            .thenReturn(SocialLinkIntent(USER_ID, LoginType.GOOGLE))
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(normalUser())
        val userInfo = GoogleOAuth2UserInfo(
            mapOf(
                "sub" to PROVIDER_ID,
                "email" to EMAIL,
                "email_verified" to false,
            ),
        )

        assertOAuth2Error("oauth2_email_not_verified") {
            service.complete(INTENT_ID, LoginType.GOOGLE, userInfo, null)
        }
    }

    @Test
    @DisplayName("다른 회원에게 연결된 Provider 계정은 연결할 수 없다")
    fun t6() {
        `when`(intentRepository.consume(INTENT_ID))
            .thenReturn(SocialLinkIntent(USER_ID, LoginType.GOOGLE))
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(normalUser())
        `when`(
            userRepository.existsBySocialProviderAndSocialProviderIdAndDeletedAtIsNull(
                LoginType.GOOGLE,
                PROVIDER_ID,
            ),
        ).thenReturn(true)

        assertOAuth2Error("oauth2_account_already_used") {
            service.complete(INTENT_ID, LoginType.GOOGLE, googleUserInfo(EMAIL), null)
        }
    }

    @Test
    @DisplayName("소셜 계정 연동 해제 시 Provider 연결 해제 후 회원의 소셜 정보를 제거한다")
    fun t7() {
        val user = normalUser().apply {
            linkSocialAccount(LoginType.NAVER, PROVIDER_ID, OAUTH_REFRESH_TOKEN)
        }
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(user)
        `when`(unlinkService.unlink(LoginType.NAVER, OAUTH_REFRESH_TOKEN)).thenReturn(true)

        service.unlink(USER_ID)

        verify(unlinkService).unlink(LoginType.NAVER, OAUTH_REFRESH_TOKEN)
        assertThat(user.socialProvider).isNull()
        assertThat(user.socialProviderId).isNull()
        assertThat(user.oauthRefreshToken).isNull()
    }

    @Test
    @DisplayName("소셜 로그인으로 가입한 회원은 유일한 로그인 수단인 소셜 연동을 해제할 수 없다")
    fun t8() {
        val user = User.createOAuth(
            loginId = "KAKAO_$PROVIDER_ID",
            email = EMAIL,
            password = "random-encoded-password",
            name = "카카오회원",
            loginType = LoginType.KAKAO,
            providerId = PROVIDER_ID,
            oauthRefreshToken = OAUTH_REFRESH_TOKEN,
        )
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(user)

        assertThatThrownBy { service.unlink(USER_ID) }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.OAUTH_UNLINK_NOT_ALLOWED)
            }

        assertThat(user.socialProvider).isEqualTo(LoginType.KAKAO)
        assertThat(user.socialProviderId).isEqualTo(PROVIDER_ID)
        assertThat(user.oauthRefreshToken).isEqualTo(OAUTH_REFRESH_TOKEN)
    }

    private fun normalUser(): User =
        User.create(
            loginId = "normal-user",
            email = EMAIL,
            password = "encoded-password",
            name = "일반회원",
            loginType = LoginType.NORMAL,
        )

    private fun googleUserInfo(email: String): GoogleOAuth2UserInfo =
        GoogleOAuth2UserInfo(
            mapOf(
                "sub" to PROVIDER_ID,
                "email" to email,
                "email_verified" to true,
            ),
        )

    private fun assertOAuth2Error(errorCode: String, action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOfSatisfying(
                org.springframework.security.oauth2.core.OAuth2AuthenticationException::class.java,
            ) {
                assertThat(it.error.errorCode).isEqualTo(errorCode)
            }
    }

    companion object {
        private const val USER_ID = 1L
        private const val INTENT_ID = "intent-id"
        private const val PROVIDER_ID = "provider-id"
        private const val EMAIL = "user@example.com"
        private const val OAUTH_REFRESH_TOKEN = "oauth-refresh-token"
    }
}
