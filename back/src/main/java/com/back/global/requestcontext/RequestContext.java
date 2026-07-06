package com.back.global.requestcontext;

import com.back.global.security.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RequestContext {
    private final HttpServletRequest req;
    private final HttpServletResponse resp;

    @Value("${custom.jwt.refreshToken.expirationSeconds}")
    private int refreshTokenExpireSeconds;

    public SecurityUser getActor() {
        return (SecurityUser) Optional.ofNullable(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                )
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof SecurityUser).orElse(null);
    }

    public String getHeader(String name, String defaultValue) {
        return Optional
                .ofNullable(req.getHeader(name))
                .filter(headerValue -> !headerValue.isBlank())
                .orElse(defaultValue);
    }

    public void setHeader(String name, String value) {
        if (value == null) value = "";

        if (value.isBlank()) {
            req.removeAttribute(name);
        } else {
            resp.setHeader(name, value);
        }
    }

    public String getCookieValue(String name, String defaultValue) {
        return Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(defaultValue);
    }

    public void setCookieWithMaxAge(String name, String value, String path, int maxAge) {
        if (value == null) value = "";

        Cookie cookie = new Cookie(name, value);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // localhost 테스트
        cookie.setAttribute("SameSite", "Lax");

        if (value.isBlank()) cookie.setMaxAge(0);
        else cookie.setMaxAge(maxAge);

        resp.addCookie(cookie);
    }

    public void setCookie(String name, String value, String path) {
        setCookieWithMaxAge(name, value, path, refreshTokenExpireSeconds);
    }

    public void deleteCookie(String name, String path) {
        setCookie(name, null, path);
    }

    public String getClientIp() {
        String forwardedFor = getHeader("X-Forwarded-For", "");

        if (!forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return req.getRemoteAddr();
    }
}
