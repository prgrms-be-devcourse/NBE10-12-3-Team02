package com.back.domain.auth.controller

import com.back.domain.auth.dto.AuthRestoreResponse
import com.back.domain.auth.dto.CsrfTokenResponse
import com.back.domain.auth.dto.EmailVerificationConfirmRequest
import com.back.domain.auth.dto.EmailVerificationRequest
import com.back.domain.auth.dto.EmailVerificationResponse
import com.back.domain.auth.dto.LoginRequest
import com.back.domain.auth.service.AuthService
import com.back.domain.auth.service.EmailVerificationService
import com.back.global.annotation.ApiV1
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.requestcontext.RequestContext
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.security.web.csrf.CsrfTokenRepository

@ApiV1
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Auth API")
class AuthController(
    private val requestContext: RequestContext,
    private val authService: AuthService,
    private val emailVerificationService: EmailVerificationService,
    private val csrfTokenRepository: CsrfTokenRepository,
) {

    @GetMapping("/csrf")
    @Operation(summary = "CSRF 토큰 발급", description = "쿠키 기반 인증 API에 사용할 CSRF 토큰을 발급합니다.")
    fun csrf(request: HttpServletRequest, response: HttpServletResponse): RsData<CsrfTokenResponse> {
        val csrfToken = csrfTokenRepository.loadToken(request)
            ?: csrfTokenRepository.generateToken(request).also {
                csrfTokenRepository.saveToken(it, request, response)
            }

        return RsData("200-1", "CSRF 토큰이 발급되었습니다.", CsrfTokenResponse(csrfToken.token))
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인 API")
    fun login(@RequestBody @Valid request: LoginRequest): RsData<Void> {
        val tokenResponse = authService.login(request.id, request.password)

        val accessToken = tokenResponse.accessToken
        val refreshToken = tokenResponse.refreshToken

        requestContext.setCookie("refreshToken", refreshToken, "/api/v1/auth")
        requestContext.setHeader("Authorization", "Bearer $accessToken")

        return RsData(
            "200-1",
            "로그인 성공 및 인증 토큰이 발급되었습니다."
        )
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그아웃 API")
    fun logout(@RequestHeader(value = "Authorization", required = false) authorization: String?): RsData<Void> {
        val refreshToken = requestContext.getCookieValue("refreshToken", "")
        authService.logout(refreshToken, authorization)

        requestContext.deleteCookie("refreshToken", AUTH_COOKIE_PATH)

        return RsData(
            "200-1",
            "로그아웃이 완료되었습니다. 토큰 및 세션 정보가 무효화되었습니다."
        )
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "토큰 재발급 API")
    fun refresh(): RsData<Void> {
        val refreshToken = requestContext.getCookieValue("refreshToken", "")

        if (refreshToken.isBlank()) {
            throw ServiceException(ErrorCode.AUTH_LOGIN_REQUIRED)
        }

        val tokenResponse = authService.refresh(refreshToken)

        requestContext.setCookie("refreshToken", tokenResponse.refreshToken, AUTH_COOKIE_PATH)
        requestContext.setHeader("Authorization", "Bearer ${tokenResponse.accessToken}")

        return RsData(
            "200-1",
            "Access Token이 정상적으로 재발급되었습니다."
        )
    }

    @PostMapping("/restore")
    @Operation(summary = "새로고침", description = "새로고침 API")
    fun restore(): RsData<AuthRestoreResponse> {
        val refreshToken = requestContext.getCookieValue("refreshToken", "")

        return try {
            val accessToken = authService.restore(refreshToken)
            requestContext.setHeader("Authorization", "Bearer $accessToken")

            RsData("200-2", "로그인 상태가 복구되었습니다.", AuthRestoreResponse(true))
        } catch (e: ServiceException) {
            handleRestoreFailure(e)
        }
    }

    @PostMapping("/email-verifications")
    @Operation(summary = "이메일 인증번호 발송", description = "회원가입에 사용할 이메일 인증번호를 발송합니다.")
    fun sendEmailVerification(
        @RequestBody @Valid request: EmailVerificationRequest,
    ): RsData<Void> {
        emailVerificationService.sendVerificationCode(request.email)
        return RsData("200-1", "이메일 인증번호를 발송했습니다.")
    }

    @PostMapping("/email-verifications/confirm")
    @Operation(summary = "이메일 인증번호 확인", description = "인증번호를 확인하고 일회용 이메일 인증 토큰을 발급합니다.")
    fun confirmEmailVerification(
        @RequestBody @Valid request: EmailVerificationConfirmRequest,
    ): RsData<EmailVerificationResponse> {
        val verificationToken = emailVerificationService.confirm(request.email, request.code)
        return RsData(
            "200-1",
            "이메일 인증이 완료되었습니다.",
            EmailVerificationResponse(verificationToken),
        )
    }

    private fun handleRestoreFailure(e: ServiceException): RsData<AuthRestoreResponse> {
        when (e.errorCode) {
            ErrorCode.AUTH_REFRESH_TOKEN_ROTATION_FAILED -> throw e
            ErrorCode.AUTH_REFRESH_TOKEN_MISMATCH -> {
                log.warn(
                    "Refresh token mismatch detected during restore. clientIp={}, userAgent={}",
                    requestContext.getClientIp(),
                    requestContext.getHeader("User-Agent", ""),
                )
                throw e
            }
            else -> {
                requestContext.deleteCookie("refreshToken", AUTH_COOKIE_PATH)
                return RsData("200-1", "비로그인 상태입니다.", AuthRestoreResponse(false))
            }
        }
    }

    companion object {
        private const val AUTH_COOKIE_PATH = "/api/v1/auth"
        private val log = LoggerFactory.getLogger(AuthController::class.java)
    }
}
