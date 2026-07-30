package com.back.domain.auth.service

import com.back.domain.auth.entity.UserSocialAuth
import com.back.domain.auth.repository.UserSocialAuthRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.security.oauth2.info.OAuth2UserInfo
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OAuth2UserAccountService(
    private val userRepository: UserRepository,
    private val userSocialAuthRepository: UserSocialAuthRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun getOrCreateUser(
        userInfo: OAuth2UserInfo,
        loginType: LoginType,
        refreshToken: String,
    ): User {
        val platformId = userInfo.providerId
            ?.takeIf { it.isNotBlank() }
            ?: throw OAuth2AuthenticationException("oauth2_provider_id_missing")
        val email = userInfo.email
            ?.takeIf { it.isNotBlank() }
            ?: throw OAuth2AuthenticationException("oauth2_email_missing")
        val loginId = "${loginType.name}_$platformId"

        val existingSocialAuth =
            userSocialAuthRepository.findByProviderAndProviderId(loginType, platformId)
        val existingUser = existingSocialAuth?.user
            ?: userRepository.findByLoginIdAndDeletedAtIsNull(loginId)

        if (existingUser != null) {
            val socialAuth = existingSocialAuth
                ?: saveSocialAuth(
                    UserSocialAuth(
                        user = existingUser,
                        provider = loginType,
                        providerId = platformId,
                        oauthRefreshToken = refreshToken.ifBlank { null },
                    ),
                )
            if (refreshToken.isNotBlank()) {
                socialAuth.updateRefreshToken(refreshToken)
            }
            return existingUser
        }

        return createOAuthUser(
            loginId = loginId,
            email = email,
            name = userInfo.name,
            loginType = loginType,
            providerId = platformId,
            refreshToken = refreshToken,
        )
    }

    private fun createOAuthUser(
        loginId: String,
        email: String,
        name: String,
        loginType: LoginType,
        providerId: String,
        refreshToken: String,
    ): User {
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw OAuth2AuthenticationException("oauth2_email_already_exists")
        }

        val encodedPassword = passwordEncoder.encode(UUID.randomUUID().toString())
        val randomPassword = requireNotNull(encodedPassword) { "Password encoding failed" }
        val user = User.create(
            loginId = loginId,
            email = email,
            password = randomPassword,
            name = name,
            loginType = loginType,
        )

        val savedUser = try {
            userRepository.saveAndFlush(user)
        } catch (e: DataIntegrityViolationException) {
            throw OAuth2AuthenticationException("oauth2_email_already_exists")
        }

        saveSocialAuth(
            UserSocialAuth(
                user = savedUser,
                provider = loginType,
                providerId = providerId,
                oauthRefreshToken = refreshToken.ifBlank { null },
            ),
        )
        return savedUser
    }

    private fun saveSocialAuth(socialAuth: UserSocialAuth): UserSocialAuth =
        try {
            userSocialAuthRepository.saveAndFlush(socialAuth)
        } catch (e: DataIntegrityViolationException) {
            throw OAuth2AuthenticationException("oauth2_account_already_used")
        }
}
