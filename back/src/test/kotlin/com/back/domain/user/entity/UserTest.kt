package com.back.domain.user.entity

import com.back.domain.user.constant.LoginType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UserTest {

    @Test
    @DisplayName("일반 회원에게 소셜 계정을 연결한다")
    fun t1() {
        val user = createNormalUser()

        user.linkSocialAccount(LoginType.KAKAO, "kakao-user-id", "oauth-refresh-token")

        assertThat(user.loginType).isEqualTo(LoginType.NORMAL)
        assertThat(user.loginId).isEqualTo("normal-user")
        assertThat(user.socialProvider).isEqualTo(LoginType.KAKAO)
        assertThat(user.socialProviderId).isEqualTo("kakao-user-id")
        assertThat(user.oauthRefreshToken).isEqualTo("oauth-refresh-token")
    }

    @Test
    @DisplayName("이미 소셜 계정이 연결된 회원에게 다른 소셜 계정을 연결할 수 없다")
    fun t2() {
        val user = createNormalUser()
        user.linkSocialAccount(LoginType.KAKAO, "kakao-user-id", null)

        assertThatThrownBy {
            user.linkSocialAccount(LoginType.GOOGLE, "google-user-id", null)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("NORMAL 타입은 소셜 제공자로 연결할 수 없다")
    fun t3() {
        val user = createNormalUser()

        assertThatThrownBy {
            user.linkSocialAccount(LoginType.NORMAL, "normal-user-id", null)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("소셜 계정 연결을 해제하면 소셜 정보와 제공자 Refresh Token을 제거한다")
    fun t4() {
        val user = createNormalUser()
        user.linkSocialAccount(LoginType.NAVER, "naver-user-id", "oauth-refresh-token")

        user.unlinkSocialAccount()

        assertThat(user.socialProvider).isNull()
        assertThat(user.socialProviderId).isNull()
        assertThat(user.oauthRefreshToken).isNull()
    }

    @Test
    @DisplayName("OAuth 회원 생성 시 소셜 계정 식별 정보를 함께 저장한다")
    fun t5() {
        val user = User.createOAuth(
            loginId = "GOOGLE_google-user-id",
            email = "google@example.com",
            password = "encoded-password",
            name = "구글회원",
            loginType = LoginType.GOOGLE,
            providerId = "google-user-id",
            oauthRefreshToken = "oauth-refresh-token",
        )

        assertThat(user.socialProvider).isEqualTo(LoginType.GOOGLE)
        assertThat(user.socialProviderId).isEqualTo("google-user-id")
        assertThat(user.oauthRefreshToken).isEqualTo("oauth-refresh-token")
    }

    private fun createNormalUser(): User =
        User.create(
            loginId = "normal-user",
            email = "normal@example.com",
            password = "encoded-password",
            name = "일반회원",
            loginType = LoginType.NORMAL,
        )
}
