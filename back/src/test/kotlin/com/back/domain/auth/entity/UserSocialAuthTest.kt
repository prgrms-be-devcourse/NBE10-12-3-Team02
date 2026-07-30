package com.back.domain.auth.entity

import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UserSocialAuthTest {
    @Test
    @DisplayName("회원의 소셜 인증 정보를 생성한다")
    fun t1() {
        val user = normalUser()

        val socialAuth = UserSocialAuth(
            user = user,
            provider = LoginType.KAKAO,
            providerId = "kakao-user-id",
            oauthRefreshToken = "oauth-refresh-token",
        )

        assertThat(socialAuth.user).isSameAs(user)
        assertThat(socialAuth.provider).isEqualTo(LoginType.KAKAO)
        assertThat(socialAuth.providerId).isEqualTo("kakao-user-id")
        assertThat(socialAuth.oauthRefreshToken).isEqualTo("oauth-refresh-token")
    }

    @Test
    @DisplayName("NORMAL 타입은 소셜 제공자로 저장할 수 없다")
    fun t2() {
        assertThatThrownBy {
            UserSocialAuth(
                user = normalUser(),
                provider = LoginType.NORMAL,
                providerId = "normal-user-id",
                oauthRefreshToken = null,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("Provider ID는 비어 있을 수 없다")
    fun t3() {
        assertThatThrownBy {
            UserSocialAuth(
                user = normalUser(),
                provider = LoginType.NAVER,
                providerId = " ",
                oauthRefreshToken = null,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("새 Provider Refresh Token으로 갱신한다")
    fun t4() {
        val socialAuth = UserSocialAuth(
            user = normalUser(),
            provider = LoginType.GOOGLE,
            providerId = "google-user-id",
            oauthRefreshToken = null,
        )

        socialAuth.updateRefreshToken("new-refresh-token")

        assertThat(socialAuth.oauthRefreshToken).isEqualTo("new-refresh-token")
    }

    private fun normalUser(): User =
        User.create(
            loginId = "normal-user",
            email = "normal@example.com",
            password = "encoded-password",
            name = "일반회원",
            loginType = LoginType.NORMAL,
        )
}
