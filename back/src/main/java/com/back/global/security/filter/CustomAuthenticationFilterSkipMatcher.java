package com.back.global.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationFilterSkipMatcher {
    public boolean shouldSkip(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        return "OPTIONS".equals(method)
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || ("GET".equals(method) && (
                path.matches("/api/[^/]+/concerts")
                        || path.matches("/api/[^/]+/concerts/\\d+")
                        || path.matches("/api/[^/]+/schedules/.*")
                        || path.matches("/api/[^/]+/users/check-id")
        ))
                || ("POST".equals(method) && (
                path.matches("/api/[^/]+/auth/login")
                        || path.matches("/api/[^/]+/auth/refresh")
                        || path.matches("/api/[^/]+/auth/logout")
                        || path.matches("/api/[^/]+/users/signup")
                        || path.matches("/api/[^/]+/auth/restore")
        ));
    }
}