package com.back.global.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Profile("dev", "test")
class TestDelayFilter : OncePerRequestFilter() {

    companion object {
        private const val MAX_DELAY_MS = 30000L // 최대 30초로 제한
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        request.getHeader("X-Test-Delay")
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.coerceAtMost(MAX_DELAY_MS)
            ?.let { delayMs -> Thread.sleep(delayMs) }

        filterChain.doFilter(request, response)
    }
}
