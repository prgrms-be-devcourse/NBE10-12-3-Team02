package com.back.global.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationFilterSkipMatcher {
    public boolean shouldSkip(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        return "OPTIONS".equals(method)
                || ("POST".equals(method) && (
                path.matches("/api/[^/]+/auth/login")
                        || path.matches("/api/[^/]+/auth/refresh")
                        || path.matches("/api/[^/]+/auth/logout")
                        || path.matches("/api/[^/]+/users/signin")
        ));
    }
}