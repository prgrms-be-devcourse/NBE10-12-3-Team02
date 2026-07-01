package com.back.global.requestcontext;

import com.back.domain.user.entity.User;
import com.back.global.security.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

    public User getActor() {
        return Optional.ofNullable(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                )
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof SecurityUser)
                .map(principal -> (SecurityUser) principal)
                .map(securityUser -> User.create(securityUser.getId(), securityUser.getName()))
                .orElse(null);
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

    public void setCookie(String name, String value) {
        setCookie(name, value, "/");
    }

    public void setCookie(String name, String value, String path) {
        if (value == null) value = "";

        Cookie cookie = new Cookie(name, value);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // localhost 테스트
        cookie.setAttribute("SameSite", "Lax");

        if (value.isBlank()) cookie.setMaxAge(0);
        else cookie.setMaxAge(60 * 60 * 24 * 365);

        resp.addCookie(cookie);
    }

    public void deleteCookie(String name, String path) {
        setCookie(name, null, path);
    }
}
