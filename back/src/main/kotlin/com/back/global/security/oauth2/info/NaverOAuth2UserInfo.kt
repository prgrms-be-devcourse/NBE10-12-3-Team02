package com.back.global.security.oauth2.info

class NaverOAuth2UserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override val providerId: String?
        get() = responseValue("id")

    override val email: String?
        get() = responseValue("email")

    override val name: String
        get() = responseValue("name")
            ?.takeIf { it.isNotBlank() }
            ?: responseValue("nickname")
                ?.takeIf { it.isNotBlank() }
            ?: "네이버사용자"

    // 네이버 프로필 API는 별도의 이메일 인증 여부 필드를 제공하지 않는다.
    // 사용자 동의 후 제공된 계정 이메일의 존재 여부를 연동 검증 기준으로 사용한다.
    override val isEmailVerified: Boolean
        get() = !email.isNullOrBlank()

    private val response: Map<*, *>
        get() = attributes["response"] as? Map<*, *> ?: emptyMap<Any, Any>()

    private fun responseValue(key: String): String? =
        response[key]?.toString()
}
