package com.back.global.security.filter

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationFilterSkipMatcher {

    fun shouldSkip(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        val method = request.method

        return method == "OPTIONS" ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/login/oauth2/") ||
                (method == "GET" && (
                        path.matches(CONCERTS_PATTERN) ||
                                path.matches(CONCERT_DETAIL_PATTERN) ||
                                path.matches(SCHEDULES_PATTERN) ||
                                path.matches(USER_CHECK_ID_PATTERN)
                        )) ||
                (method == "POST" && (
                        path.matches(AUTH_LOGIN_PATTERN) ||
                                path.matches(AUTH_REFRESH_PATTERN) ||
                                path.matches(AUTH_LOGOUT_PATTERN) ||
                                path.matches(USER_SIGNUP_PATTERN) ||
                                path.matches(AUTH_RESTORE_PATTERN) ||
                                path.matches(EMAIL_VERIFICATION_PATTERN) ||
                                path.matches(EMAIL_VERIFICATION_CONFIRM_PATTERN)
                        ))
    }

    companion object {
        private val CONCERTS_PATTERN = Regex("/api/[^/]+/concerts")
        private val CONCERT_DETAIL_PATTERN = Regex("/api/[^/]+/concerts/\\d+")
        private val SCHEDULES_PATTERN = Regex("/api/[^/]+/schedules/.*")
        private val USER_CHECK_ID_PATTERN = Regex("/api/[^/]+/users/check-id")
        private val AUTH_LOGIN_PATTERN = Regex("/api/[^/]+/auth/login")
        private val AUTH_REFRESH_PATTERN = Regex("/api/[^/]+/auth/refresh")
        private val AUTH_LOGOUT_PATTERN = Regex("/api/[^/]+/auth/logout")
        private val USER_SIGNUP_PATTERN = Regex("/api/[^/]+/users/signup")
        private val AUTH_RESTORE_PATTERN = Regex("/api/[^/]+/auth/restore")
        private val EMAIL_VERIFICATION_PATTERN = Regex("/api/[^/]+/auth/email-verifications")
        private val EMAIL_VERIFICATION_CONFIRM_PATTERN =
            Regex("/api/[^/]+/auth/email-verifications/confirm")
    }
}
