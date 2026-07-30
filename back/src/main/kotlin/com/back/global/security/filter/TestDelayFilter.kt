package com.back.global.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TestDelayFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val delayHeader = request.getHeader("X-Test-Delay")
        if (!delayHeader.isNullOrBlank()) {
            val delayMs = delayHeader.toLongOrNull() ?: 0L
            if (delayMs > 0) {
                Thread.sleep(delayMs)
            }
        }
        filterChain.doFilter(request, response)
    }
}
