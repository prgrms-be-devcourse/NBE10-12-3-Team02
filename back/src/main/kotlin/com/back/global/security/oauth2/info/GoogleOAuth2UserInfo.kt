package com.back.global.security.oauth2.info

class GoogleOAuth2UserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {

    override val providerId: String?
        get() = attributes["sub"]?.toString()

    override val email: String?
        get() = attributes["email"]?.toString()

    override val name: String
        get() = attributes["name"]?.toString() ?: "구글사용자"

    override val isEmailVerified: Boolean
        get() = attributes["email_verified"]?.toString()?.toBooleanStrictOrNull() == true
}
