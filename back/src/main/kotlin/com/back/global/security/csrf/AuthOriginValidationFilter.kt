package com.back.global.security.csrf

import com.back.global.exception.ErrorCode
import com.back.global.rsData.RsData
import com.back.global.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URI

@Component
class AuthOriginValidationFilter(
    @Value("\${custom.cors.allowed-origins:http://localhost:3000}")
    allowedOrigins: Array<String>,
) : OncePerRequestFilter() {
    private val allowedOrigins = allowedOrigins.mapNotNull(::normalizeOrigin).toSet()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !AuthCsrfRequestMatcher.matches(request)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val originHeader = request.getHeader(ORIGIN_HEADER)
        val requestOrigin = if (originHeader.isNullOrBlank()) {
            request.getHeader(REFERER_HEADER)?.let(::normalizeOrigin)
        } else {
            normalizeOrigin(originHeader)
        }

        if (requestOrigin == null || requestOrigin !in allowedOrigins) {
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write(
                Ut.json.toString(
                    RsData<Void>(ErrorCode.AUTH_FORBIDDEN.resultCode, "허용되지 않은 요청 출처입니다."),
                    "{\"resultCode\":\"403-1\",\"msg\":\"허용되지 않은 요청 출처입니다.\",\"data\":null}",
                ),
            )
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun normalizeOrigin(value: String): String? =
        runCatching {
            val uri = URI(value.trim())
            if (uri.scheme !in ALLOWED_SCHEMES || uri.host.isNullOrBlank() || uri.userInfo != null) {
                return@runCatching null
            }

            URI(uri.scheme.lowercase(), null, uri.host.lowercase(), uri.port, null, null, null).toString()
        }.getOrNull()

    companion object {
        private const val ORIGIN_HEADER = "Origin"
        private const val REFERER_HEADER = "Referer"
        private val ALLOWED_SCHEMES = setOf("http", "https")
    }
}
