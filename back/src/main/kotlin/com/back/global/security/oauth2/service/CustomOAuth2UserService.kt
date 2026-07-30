package com.back.global.security.oauth2.service

import com.back.domain.user.constant.LoginType
import com.back.domain.auth.repository.SocialLinkCookieRepository
import com.back.domain.auth.service.OAuth2UserAccountService
import com.back.domain.auth.service.SocialLinkService
import com.back.global.security.oauth2.info.GoogleOAuth2UserInfo
import com.back.global.security.oauth2.info.KakaoOAuth2UserInfo
import com.back.global.security.oauth2.info.NaverOAuth2UserInfo
import com.back.global.security.oauth2.info.OAuth2UserInfo
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val socialLinkService: SocialLinkService,
    private val socialLinkCookieRepository: SocialLinkCookieRepository,
    private val oAuth2UserAccountService: OAuth2UserAccountService,
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId
        val refreshToken = userRequest.additionalParameters.getOrDefault("refresh_token", "").toString()

        @Suppress("UNCHECKED_CAST")
        val attributes = oAuth2User.attributes as Map<String, Any>

        val (loginType, userInfo) = when (registrationId) {
            "kakao" -> LoginType.KAKAO to createUserInfo(attributes, LoginType.KAKAO)
            "naver" -> LoginType.NAVER to createUserInfo(oAuth2User.attributes, LoginType.NAVER)
            "google" -> LoginType.GOOGLE to createUserInfo(attributes, LoginType.GOOGLE)
            else -> throw OAuth2AuthenticationException("oauth2_provider_not_supported")
        }
        val linkIntentId = socialLinkCookieRepository.load()
        val isSocialLink = linkIntentId != null
        val user = linkIntentId
            ?.let { intentId ->
                socialLinkService.complete(
                    intentId = intentId,
                    provider = loginType,
                    userInfo = userInfo,
                    oauthRefreshToken = refreshToken.ifBlank { null },
                )
            }
            ?: oAuth2UserAccountService.getOrCreateUser(userInfo, loginType, refreshToken)

        val userId = user.userId ?: throw IllegalStateException("User ID must not be null")

        return DefaultOAuth2User(
            oAuth2User.authorities,
            mapOf(
                "userId" to userId,
                "loginId" to user.loginId,
                "email" to user.email,
                "name" to user.name,
                "socialLink" to isSocialLink,
            ),
            "userId"
        )
    }

    private fun createUserInfo(attributes: Map<String, Any>, loginType: LoginType): OAuth2UserInfo =
        when (loginType.name) {
            "KAKAO" -> KakaoOAuth2UserInfo(attributes)
            "NAVER" -> NaverOAuth2UserInfo(attributes)
            "GOOGLE" -> GoogleOAuth2UserInfo(attributes)
            else -> throw OAuth2AuthenticationException("oauth2_provider_not_supported")
        }
}
