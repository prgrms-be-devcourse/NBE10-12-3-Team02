package com.back.domain.auth.service

import com.back.domain.auth.entity.UserSocialAuth
import com.back.domain.auth.repository.UserSocialAuthRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.security.oauth2.info.GoogleOAuth2UserInfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.test.util.ReflectionTestUtils

class OAuth2UserAccountServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val userSocialAuthRepository = mock(UserSocialAuthRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val service = OAuth2UserAccountService(
        userRepository,
        userSocialAuthRepository,
        passwordEncoder,
    )

    @Test
    @DisplayName("Provider 계정이 연결된 기존 회원을 조회한다")
    fun t1() {
        val existingUser = oauthUser()
        // 실제로는 영속화된 User를 참조하는 지연 로딩 프록시라 userId가 항상 채워져 있다.
        ReflectionTestUtils.setField(existingUser, "userId", USER_ID)
        val socialAuth = socialAuth(existingUser)
        `when`(userSocialAuthRepository.findByProviderAndProviderId(LoginType.GOOGLE, PROVIDER_ID))
            .thenReturn(socialAuth)
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(USER_ID))
            .thenReturn(existingUser)

        val result = service.getOrCreateUser(userInfo(), LoginType.GOOGLE, "")

        assertThat(result).isSameAs(existingUser)
        verify(userRepository, never()).save(existingUser)
    }

    @Test
    @DisplayName("기존 OAuth 회원의 소셜 필드와 새 Refresh Token을 보완한다")
    fun t2() {
        val existingUser = User.create(
            loginId = "GOOGLE_$PROVIDER_ID",
            email = EMAIL,
            password = "encoded-password",
            name = NAME,
            loginType = LoginType.GOOGLE,
        )
        `when`(userSocialAuthRepository.findByProviderAndProviderId(LoginType.GOOGLE, PROVIDER_ID))
            .thenReturn(null)
        `when`(userRepository.findByLoginIdAndDeletedAtIsNull("GOOGLE_$PROVIDER_ID"))
            .thenReturn(existingUser)
        `when`(userSocialAuthRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(UserSocialAuth::class.java)))
            .thenAnswer { it.arguments[0] as UserSocialAuth }

        val result = service.getOrCreateUser(
            userInfo(),
            LoginType.GOOGLE,
            OAUTH_REFRESH_TOKEN,
        )

        assertThat(result).isSameAs(existingUser)
        verify(userSocialAuthRepository).saveAndFlush(
            org.mockito.ArgumentMatchers.any(UserSocialAuth::class.java),
        )
    }

    @Test
    @DisplayName("기존 회원이 없으면 신규 OAuth 회원을 생성한다")
    fun t3() {
        `when`(userSocialAuthRepository.findByProviderAndProviderId(LoginType.GOOGLE, PROVIDER_ID))
            .thenReturn(null)
        `when`(userRepository.findByLoginIdAndDeletedAtIsNull("GOOGLE_$PROVIDER_ID"))
            .thenReturn(null)
        `when`(userRepository.existsByEmailAndDeletedAtIsNull(EMAIL)).thenReturn(false)
        `when`(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password")
        `when`(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User::class.java)))
            .thenAnswer { it.arguments[0] as User }
        var savedSocialAuth: UserSocialAuth? = null
        `when`(userSocialAuthRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(UserSocialAuth::class.java)))
            .thenAnswer {
                (it.arguments[0] as UserSocialAuth).also { auth -> savedSocialAuth = auth }
            }

        val result = service.getOrCreateUser(
            userInfo(),
            LoginType.GOOGLE,
            OAUTH_REFRESH_TOKEN,
        )

        assertThat(result.loginId).isEqualTo("GOOGLE_$PROVIDER_ID")
        assertThat(result.loginType).isEqualTo(LoginType.GOOGLE)
        assertThat(savedSocialAuth?.provider).isEqualTo(LoginType.GOOGLE)
        assertThat(savedSocialAuth?.providerId).isEqualTo(PROVIDER_ID)
        assertThat(savedSocialAuth?.oauthRefreshToken).isEqualTo(OAUTH_REFRESH_TOKEN)
    }

    @Test
    @DisplayName("Provider 사용자 ID가 없으면 OAuth2 인증 실패로 처리한다")
    fun t4() {
        val userInfo = GoogleOAuth2UserInfo(
            mapOf(
                "email" to EMAIL,
                "name" to NAME,
                "email_verified" to true,
            ),
        )

        assertOAuth2Error("oauth2_provider_id_missing") {
            service.getOrCreateUser(userInfo, LoginType.GOOGLE, "")
        }
    }

    @Test
    @DisplayName("신규 OAuth 회원의 이메일이 이미 사용 중이면 가입을 거절한다")
    fun t5() {
        `when`(userSocialAuthRepository.findByProviderAndProviderId(LoginType.GOOGLE, PROVIDER_ID))
            .thenReturn(null)
        `when`(userRepository.findByLoginIdAndDeletedAtIsNull("GOOGLE_$PROVIDER_ID"))
            .thenReturn(null)
        `when`(userRepository.existsByEmailAndDeletedAtIsNull(EMAIL)).thenReturn(true)

        assertOAuth2Error("oauth2_email_already_exists") {
            service.getOrCreateUser(userInfo(), LoginType.GOOGLE, "")
        }
    }

    private fun oauthUser(): User =
        User.create(
            loginId = "GOOGLE_$PROVIDER_ID",
            email = EMAIL,
            password = "encoded-password",
            name = NAME,
            loginType = LoginType.GOOGLE,
        )

    private fun socialAuth(user: User): UserSocialAuth =
        UserSocialAuth(
            user = user,
            provider = LoginType.GOOGLE,
            providerId = PROVIDER_ID,
            oauthRefreshToken = null,
        )

    private fun userInfo(): GoogleOAuth2UserInfo =
        GoogleOAuth2UserInfo(
            mapOf(
                "sub" to PROVIDER_ID,
                "email" to EMAIL,
                "name" to NAME,
                "email_verified" to true,
            ),
        )

    private fun assertOAuth2Error(errorCode: String, action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOfSatisfying(OAuth2AuthenticationException::class.java) {
                assertThat(it.error.errorCode).isEqualTo(errorCode)
            }
    }

    companion object {
        private const val USER_ID = 1L
        private const val PROVIDER_ID = "google-provider-id"
        private const val EMAIL = "user@example.com"
        private const val NAME = "구글회원"
        private const val OAUTH_REFRESH_TOKEN = "oauth-refresh-token"
    }
}
