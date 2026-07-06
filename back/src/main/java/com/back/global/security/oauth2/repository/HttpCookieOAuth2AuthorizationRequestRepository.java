package com.back.global.security.oauth2.repository;

import com.back.global.requestcontext.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_COOKIE_NAME = "oauth2_auth_request";
    private static final String OAUTH2_COOKIE_PATH = "/";
    private final RequestContext requestContext;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String value = requestContext.getCookieValue(
                OAUTH2_COOKIE_NAME,
                ""
        );

        if (value.isBlank()) return null;

        try {
            return deserialize(value);
        } catch (RuntimeException e) {
            requestContext.deleteCookie(OAUTH2_COOKIE_NAME, OAUTH2_COOKIE_PATH);
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            requestContext.deleteCookie(OAUTH2_COOKIE_NAME, OAUTH2_COOKIE_PATH);
            return;
        }

        requestContext.setCookieWithMaxAge(
                OAUTH2_COOKIE_NAME,
                serialize(authorizationRequest),
                OAUTH2_COOKIE_PATH,
                180
        );
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        requestContext.deleteCookie(OAUTH2_COOKIE_NAME, OAUTH2_COOKIE_PATH);
        return authorizationRequest;
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        byte[] bytes = SerializationUtils.serialize(authorizationRequest);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
    }
}