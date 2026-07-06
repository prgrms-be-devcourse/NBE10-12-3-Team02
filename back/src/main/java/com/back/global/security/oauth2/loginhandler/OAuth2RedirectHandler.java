package com.back.global.security.oauth2.loginhandler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2RedirectHandler {
    private static final String FRONT_CALLBACK_URL = "http://localhost:3000";
    private static final String FRONT_LOGIN_URL = "http://localhost:3000/login";

    public void redirectSuccess(HttpServletResponse response, String accessToken) throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(FRONT_CALLBACK_URL)
                .fragment("accessToken=" + accessToken)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    public void redirectFailure(HttpServletResponse response, String errorCode) throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(FRONT_LOGIN_URL)
                .queryParam("error", errorCode)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}