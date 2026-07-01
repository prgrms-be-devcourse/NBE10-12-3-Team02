package com.back.global.security.filter;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenExtractor {
    private static final String BEARER_PREFIX = "Bearer ";

    public String extract(String authorization) {
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_BEARER_HEADER);
        }

        String accessToken = authorization.substring(BEARER_PREFIX.length()).trim();

        if (accessToken.isBlank()) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_BEARER_HEADER);
        }

        return accessToken;
    }
}