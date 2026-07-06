package com.back.global.security.oauth2.loginhandler;

import com.back.domain.auth.dto.TokenResponse;
import com.back.domain.auth.service.AuthService;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.requestcontext.RequestContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final AuthService authService;
    private final RequestContext requestContext;
    private final OAuth2RedirectHandler redirectHandler;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Object userIdAttribute = oAuth2User.getAttribute("userId");

        if (userIdAttribute == null) {
            redirectHandler.redirectFailure(response, "oauth2_user_id_missing");
            return;
        }

        Long userId;

        try {
            userId = Long.valueOf(userIdAttribute.toString());
        } catch (NumberFormatException e) {
            redirectHandler.redirectFailure(response, "oauth2_user_id_invalid");
            return;
        }

        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElse(null);

        if (user == null) {
            redirectHandler.redirectFailure(response, "oauth2_user_not_found");
            return;
        }

        try {
            TokenResponse tokenResponse = authService.issueTokens(user);
            requestContext.setCookie("refreshToken", tokenResponse.refreshToken(), "/api/v1/auth");
            redirectHandler.redirectSuccess(response, tokenResponse.accessToken());
        } catch (RuntimeException e) {
            redirectHandler.redirectFailure(response, "oauth2_token_issue_failed");
        }
    }
}