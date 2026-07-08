package com.back.domain.auth.controller;

import com.back.domain.auth.dto.AuthRestoreResponse;
import com.back.domain.auth.dto.LoginRequest;
import com.back.domain.auth.dto.TokenResponse;
import com.back.domain.auth.service.AuthService;
import com.back.global.annotation.ApiV1;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.requestcontext.RequestContext;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@ApiV1
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Auth API")
public class AuthController {
    private final RequestContext requestContext;
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인 API")
    public RsData<Void> login(@RequestBody @Valid LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request.id(), request.password());

        String accessToken = tokenResponse.accessToken();
        String refreshToken = tokenResponse.refreshToken();

        requestContext.setCookie("refreshToken", refreshToken, "/api/v1/auth");
        requestContext.setHeader("Authorization", "Bearer " + accessToken);

        return new RsData<>(
                "200-1",
                "로그인 성공 및 인증 토큰이 발급되었습니다."
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그아웃 API")
    public RsData<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {

        String refreshToken = requestContext.getCookieValue("refreshToken", "");
        authService.logout(refreshToken, authorization);

        requestContext.deleteCookie("refreshToken", "/api/v1/auth");

        return new RsData<>(
                "200-1",
                "로그아웃이 완료되었습니다. 토큰 및 세션 정보가 무효화되었습니다."
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "토큰 재발급 API")
    public RsData<Void> refresh() {
        String refreshToken = requestContext.getCookieValue("refreshToken", "");

        if (refreshToken.isBlank()) {
            throw new ServiceException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        TokenResponse tokenResponse = authService.refresh(refreshToken);

        requestContext.setCookie("refreshToken", tokenResponse.refreshToken(), "/api/v1/auth");
        requestContext.setHeader("Authorization", "Bearer " + tokenResponse.accessToken());

        return new RsData<>(
                "200-1",
                "Access Token이 정상적으로 재발급되었습니다."
        );
    }

    @PostMapping("/restore")
    @Operation(summary = "새로고침", description = "새로고침 API")
    public RsData<AuthRestoreResponse> restore() {
        String refreshToken = requestContext.getCookieValue("refreshToken", "");

        try {
            String accessToken = authService.restore(refreshToken);
            requestContext.setHeader("Authorization", "Bearer " + accessToken);

            return new RsData<>("200-2", "로그인 상태가 복구되었습니다.", new AuthRestoreResponse(true));
        } catch (ServiceException e) {
            if (e.getErrorCode() == ErrorCode.AUTH_REFRESH_TOKEN_ROTATION_FAILED) {
                throw e;
            }

            if (e.getErrorCode() == ErrorCode.AUTH_REFRESH_TOKEN_MISMATCH) {
                log.warn(
                        "Refresh token mismatch detected during restore. clientIp={}, userAgent={}",
                        requestContext.getClientIp(),
                        requestContext.getHeader("User-Agent", "")
                );
                throw e;
            }

            requestContext.deleteCookie("refreshToken", "/api/v1/auth");
            return new RsData<>("200-1", "비로그인 상태입니다.", new AuthRestoreResponse(false));
        }
    }
}
