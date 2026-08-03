package com.back.global.security.filter

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.requestcontext.RequestContext
import com.back.global.security.auth.SecurityAuthenticationFactory
import com.back.global.security.jwt.JwtTokenProvider
import com.back.global.security.jwt.repository.BlacklistRepository
import com.back.global.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CustomAuthenticationFilter(
    private val requestContext: RequestContext,
    private val jwtTokenProvider: JwtTokenProvider,
    private val bearerTokenExtractor: BearerTokenExtractor,
    private val skipMatcher: CustomAuthenticationFilterSkipMatcher,
    private val securityAuthenticationFactory: SecurityAuthenticationFactory,
    private val blacklistRepository: BlacklistRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            authenticateByAccessToken(request)
            filterChain.doFilter(request, response)
        } catch (e: ServiceException) {
            SecurityContextHolder.clearContext()

            val rsData = e.rsData
            response.contentType = "application/json;charset=UTF-8"
            response.status = rsData.statusCode
            response.writer.write(
                Ut.json.toString(
                    rsData,
                    "{\"resultCode\":\"500-1\",\"msg\":\"JSON 변환 실패\",\"data\":null}"
                )
            )
        }
    }

    private fun authenticateByAccessToken(request: HttpServletRequest) {
        val authorization = requestContext.getHeader("Authorization", "")

        val accessToken = if (authorization.isBlank()) {
            // 브라우저 EventSource는 커스텀 헤더를 보낼 수 없으므로, SSE 구독 요청에 한해
            // 쿼리파라미터 token을 fallback으로 허용한다. 다른 엔드포인트는 영향받지 않는다.
            if (!isNotificationSubscribeRequest(request)) {
                return
            }
            request.getParameter("token")?.takeIf { it.isNotBlank() } ?: return
        } else {
            bearerTokenExtractor.extract(authorization)
        }

        if (blacklistRepository.isBlacklisted(accessToken)) {
            throw ServiceException(ErrorCode.AUTH_LOGIN_REQUIRED)
        }

        val payload = jwtTokenProvider.parseAccessToken(accessToken)
        val authentication = securityAuthenticationFactory.create(payload)
        SecurityContextHolder.getContext().authentication = authentication
    }

    // request.servletPath는 이 프로젝트의 서블릿 매핑 구성상 항상 빈 문자열로 내려오므로
    // (CustomAuthenticationFilterSkipMatcher와 달리) requestURI를 기준으로 판단한다.
    private fun isNotificationSubscribeRequest(request: HttpServletRequest): Boolean =
        request.method == "GET" && NOTIFICATION_SUBSCRIBE_PATTERN.matches(request.requestURI)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        skipMatcher.shouldSkip(request)

    companion object {
        private val NOTIFICATION_SUBSCRIBE_PATTERN = Regex("/api/[^/]+/notifications/subscribe")
    }
}
