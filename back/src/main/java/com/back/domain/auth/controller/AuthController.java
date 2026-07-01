package com.back.domain.auth.controller;

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
import org.springframework.web.bind.annotation.*;

@ApiV1
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Auth API")
public class AuthController {
    private final RequestContext rq;
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인 API")
    public RsData<Void> login(@RequestBody @Valid LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request.id(), request.password());

        String accessToken = tokenResponse.accessToken();
        String refreshToken = tokenResponse.refreshToken();

        rq.setCookie("refreshToken", refreshToken, "/api/v1/auth");
        rq.setHeader("Authorization", "Bearer " + accessToken);

        return new RsData<>(
                "200-1",
                "로그인 성공 및 인증 토큰이 발급되었습니다."
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그아웃 API")
    public RsData<Void> logout() {

        String refreshToken = rq.getCookieValue("refreshToken", "");
        authService.logout(refreshToken);

        rq.deleteCookie("refreshToken", "/api/v1/auth");

        return new RsData<>(
                "200-1",
                "로그아웃이 완료되었습니다. 토큰 및 세션 정보가 무효화되었습니다."
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "토큰 재발급 API")
    public RsData<Void> refresh() {
        String refreshToken = rq.getCookieValue("refreshToken", "");

        if (refreshToken.isBlank()) {
            throw new ServiceException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        TokenResponse tokenResponse = authService.refresh(refreshToken);

        rq.setCookie("refreshToken", tokenResponse.refreshToken(), "/api/v1/auth");
        rq.setHeader("Authorization", "Bearer " + tokenResponse.accessToken());

        return new RsData<>(
                "200-1",
                "Access Token이 정상적으로 재발급되었습니다."
        );
    }
}
