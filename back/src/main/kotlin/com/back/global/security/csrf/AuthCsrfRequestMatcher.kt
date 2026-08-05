package com.back.global.security.csrf

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.util.matcher.RequestMatcher

object AuthCsrfRequestMatcher : RequestMatcher {
    private val protectedPath = Regex("/api/[^/]+/auth/(refresh|logout|restore)")

    override fun matches(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        return request.method.equals("POST", ignoreCase = true) && protectedPath.matches(path)
    }
}
