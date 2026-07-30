package com.back.domain.auth.service

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

        val existingUser =
            userRepository.findBySocialProviderAndSocialProviderIdAndDeletedAtIsNull(
                loginType,
                platformId,
            )
                ?: userRepository.findByLoginIdAndDeletedAtIsNull(loginId)

        if (existingUser != null) {
            if (existingUser.socialProvider == null) {
                existingUser.linkSocialAccount(
                    loginType,
                    platformId,
                    refreshToken.ifBlank { null },
                )
            }
            if (refreshToken.isNotBlank()) {
                existingUser.updateOauthRefreshToken(refreshToken)
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
        val user = User.createOAuth(
            loginId = loginId,
            email = email,
            password = randomPassword,
            name = name,
            loginType = loginType,
            providerId = providerId,
            oauthRefreshToken = refreshToken.ifBlank { null },
        )

        return try {
            userRepository.save(user)
        } catch (e: DataIntegrityViolationException) {
            throw OAuth2AuthenticationException("oauth2_email_already_exists")
        }
    }
}
