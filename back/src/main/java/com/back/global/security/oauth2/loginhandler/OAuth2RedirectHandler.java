package com.back.global.security.oauth2.loginhandler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2RedirectHandler {

    @Value("${spring.security.oauth2.front-callback-url}")
    private String frontCallbackUrl;

    @Value("${spring.security.oauth2.front-login-url}")
    private String frontLoginUrl;

    public void redirectSuccess(HttpServletResponse response, String accessToken) throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontCallbackUrl)
                .fragment("accessToken=" + accessToken)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    public void redirectFailure(HttpServletResponse response, String errorCode) throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontLoginUrl)
                .queryParam("error", errorCode)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}