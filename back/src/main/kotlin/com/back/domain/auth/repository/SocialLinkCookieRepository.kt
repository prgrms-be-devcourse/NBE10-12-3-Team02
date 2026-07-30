package com.back.domain.auth.repository

import com.back.global.requestcontext.RequestContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SocialLinkCookieRepository(
    private val requestContext: RequestContext,
    @Value("\${custom.oauth2.link-intent-expiration-seconds:300}")
    private val expirationSeconds: Int,
) {
    fun save(intentId: String) {
        requestContext.setCookieWithMaxAge(
            COOKIE_NAME,
            intentId,
            COOKIE_PATH,
            expirationSeconds,
        )
    }

    fun load(): String? =
        requestContext.getCookieValue(COOKIE_NAME).takeIf { it.isNotBlank() }

    fun remove() {
        requestContext.deleteCookie(COOKIE_NAME, COOKIE_PATH)
    }

    companion object {
        private const val COOKIE_NAME = "oauth2_link_intent"
        private const val COOKIE_PATH = "/"
    }
}
