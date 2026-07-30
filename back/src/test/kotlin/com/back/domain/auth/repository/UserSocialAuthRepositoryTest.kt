package com.back.domain.auth.repository

import com.back.domain.auth.entity.UserSocialAuth
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import com.back.global.jpa.converter.EncryptedStringConverter
import com.back.global.util.AesEncryptionUtil

@DataJpaTest
@Import(AesEncryptionUtil::class, EncryptedStringConverter::class)
class UserSocialAuthRepositoryTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val userSocialAuthRepository: UserSocialAuthRepository,
) {
    @Test
    @DisplayName("한 회원에게 서로 다른 Provider 계정을 중복 연결할 수 없다")
    fun t1() {
        val user = saveUser("user-1", "user1@example.com")
        userSocialAuthRepository.saveAndFlush(
            socialAuth(user, LoginType.KAKAO, "kakao-id"),
        )

        assertThatThrownBy {
            userSocialAuthRepository.saveAndFlush(
                socialAuth(user, LoginType.NAVER, "naver-id"),
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    @DisplayName("동일한 Provider 계정을 서로 다른 회원에게 중복 연결할 수 없다")
    fun t2() {
        val firstUser = saveUser("user-1", "user1@example.com")
        val secondUser = saveUser("user-2", "user2@example.com")
        userSocialAuthRepository.saveAndFlush(
            socialAuth(firstUser, LoginType.GOOGLE, "shared-google-id"),
        )

        assertThatThrownBy {
            userSocialAuthRepository.saveAndFlush(
                socialAuth(secondUser, LoginType.GOOGLE, "shared-google-id"),
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    @DisplayName("사용자와 Provider 정보가 모두 일치할 때만 소셜 인증 정보를 삭제한다")
    fun t3() {
        val user = saveUser("user-1", "user1@example.com")
        val userId = requireNotNull(user.userId)
        userSocialAuthRepository.saveAndFlush(
            socialAuth(user, LoginType.NAVER, "naver-id"),
        )

        val mismatchCount = userSocialAuthRepository.deleteIfMatches(
            userId,
            LoginType.NAVER,
            "other-id",
        )
        val deletedCount = userSocialAuthRepository.deleteIfMatches(
            userId,
            LoginType.NAVER,
            "naver-id",
        )

        assertThat(mismatchCount).isZero()
        assertThat(deletedCount).isEqualTo(1)
        assertThat(userSocialAuthRepository.findByUserUserId(userId)).isNull()
    }

    private fun saveUser(loginId: String, email: String): User =
        userRepository.saveAndFlush(
            User.create(
                loginId = loginId,
                email = email,
                password = "encoded-password",
                name = "회원",
                loginType = LoginType.NORMAL,
            ),
        )

    private fun socialAuth(
        user: User,
        provider: LoginType,
        providerId: String,
    ): UserSocialAuth =
        UserSocialAuth(
            user = user,
            provider = provider,
            providerId = providerId,
            oauthRefreshToken = "oauth-refresh-token",
        )
}
