package com.back.domain.auth.service

import com.back.domain.auth.dto.SocialLinkStartResult
import com.back.domain.auth.dto.SocialLinkStatusResponse
import com.back.domain.auth.repository.SocialLinkIntentRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.security.oauth2.info.OAuth2UserInfo
import com.back.global.security.oauth2.service.OAuthUnlinkService
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

@Service
@Transactional(readOnly = true)
class SocialLinkService(
    private val userRepository: UserRepository,
    private val socialLinkIntentRepository: SocialLinkIntentRepository,
    private val oAuthUnlinkService: OAuthUnlinkService,
    private val socialLinkQueryService: SocialLinkQueryService,
    private val socialLinkCommandService: SocialLinkCommandService,
    @Value("\${custom.oauth2.link-intent-expiration-seconds:300}")
    private val linkIntentExpirationSeconds: Long,
) {
    fun start(userId: Long, providerName: String): SocialLinkStartResult {
        val provider = parseProvider(providerName)
        val user = findUser(userId)

        if (user.socialProvider != null) {
            throw ServiceException(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED)
        }

        val intentId = UUID.randomUUID().toString()
        socialLinkIntentRepository.save(
            intentId = intentId,
            userId = userId,
            provider = provider,
            ttl = Duration.ofSeconds(linkIntentExpirationSeconds),
        )

        return SocialLinkStartResult(
            intentId = intentId,
            authorizationPath = "/oauth2/authorization/${provider.name.lowercase()}",
        )
    }

    fun getStatus(userId: Long): SocialLinkStatusResponse =
        SocialLinkStatusResponse.from(findUser(userId))

    @Transactional
    fun complete(
        intentId: String,
        provider: LoginType,
        userInfo: OAuth2UserInfo,
        oauthRefreshToken: String?,
    ): User {
        val intent = socialLinkIntentRepository.consume(intentId)
            ?: throw OAuth2AuthenticationException("oauth2_link_request_expired")

        if (intent.provider != provider) {
            throw OAuth2AuthenticationException("oauth2_provider_mismatch")
        }

        val user = userRepository.findByUserIdAndDeletedAtIsNull(intent.userId)
            ?: throw OAuth2AuthenticationException("oauth2_user_not_found")

        if (user.socialProvider != null) {
            throw OAuth2AuthenticationException("oauth2_account_already_linked")
        }

        val providerId = userInfo.providerId
            ?.takeIf { it.isNotBlank() }
            ?: throw OAuth2AuthenticationException("oauth2_provider_id_missing")
        val providerEmail = userInfo.email
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw OAuth2AuthenticationException("oauth2_email_missing")

        if (!userInfo.isEmailVerified) {
            throw OAuth2AuthenticationException("oauth2_email_not_verified")
        }
        if (!providerEmail.equals(user.email.trim(), ignoreCase = true)) {
            throw OAuth2AuthenticationException("oauth2_email_mismatch")
        }
        if (
            userRepository.existsBySocialProviderAndSocialProviderIdAndDeletedAtIsNull(
                provider,
                providerId,
            )
        ) {
            throw OAuth2AuthenticationException("oauth2_account_already_used")
        }

        user.linkSocialAccount(provider, providerId, oauthRefreshToken)

        return try {
            userRepository.saveAndFlush(user)
        } catch (e: DataIntegrityViolationException) {
            throw OAuth2AuthenticationException("oauth2_account_already_used")
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun unlink(userId: Long) {
        val target = socialLinkQueryService.getUnlinkTarget(userId)

        if (!oAuthUnlinkService.unlink(target.provider, target.oauthRefreshToken)) {
            throw ServiceException(ErrorCode.OAUTH_UNLINK_FAILED)
        }

        socialLinkCommandService.completeUnlink(target)
    }

    private fun parseProvider(providerName: String): LoginType {
        val provider = runCatching {
            LoginType.valueOf(providerName.uppercase())
        }.getOrNull()

        if (provider == null || provider == LoginType.NORMAL) {
            throw ServiceException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED)
        }
        return provider
    }

    private fun findUser(userId: Long): User =
        userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)
}
