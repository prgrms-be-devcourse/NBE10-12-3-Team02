package com.back.global.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Profile("dev", "test") // 운영(prod) 환경 배포 방지
class TestDelayFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        request.getHeader("X-Test-Delay")
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.let { delayMs -> Thread.sleep(delayMs) }

        filterChain.doFilter(request, response)
    }
}
