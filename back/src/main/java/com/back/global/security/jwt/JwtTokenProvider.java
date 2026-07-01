package com.back.global.security.jwt;

import com.back.domain.user.entity.User;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.security.jwt.payload.AccessTokenPayload;
import com.back.global.security.jwt.payload.RefreshTokenPayload;
import com.back.standard.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    @Value("${custom.jwt.accessToken.secret}")
    private String accessTokenSecret;

    @Value("${custom.jwt.refreshToken.secret}")
    private String refreshTokenSecret;

    @Value("${custom.jwt.accessToken.expirationSeconds}")
    private int accessTokenExpireSeconds;

    @Value("${custom.jwt.refreshToken.expirationSeconds}")
    private int refreshTokenExpireSeconds;

    public RefreshTokenPayload parseRefreshToken(String refreshToken) {
        Map<String, Object> payload = Ut.jwt.payload(refreshTokenSecret, refreshToken);

        if (payload == null) {
            return null;
        }

        try {
            Long userId = getLongClaim(payload, "id");
            String jti = getStringClaim(payload, "jti");

            return new RefreshTokenPayload(userId, jti);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public AccessTokenPayload parseAccessToken(String accessToken) {
        Map<String, Object> payload = Ut.jwt.payload(accessTokenSecret, accessToken);

        if (payload == null) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        try {
            Long userId = getLongClaim(payload, "id");
            String name = getStringClaim(payload, "name");

            return new AccessTokenPayload(userId, name);
        } catch (RuntimeException e) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    private Long getLongClaim(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        if (value == null) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    private String getStringClaim(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Missing claim: " + key);
        }

        return value.toString();
    }

    public String createAccessToken(User user) {
        return Ut.jwt.toString(
                accessTokenSecret,
                accessTokenExpireSeconds,
                Map.of(
                        "id", user.getUserId(),
                        "name", user.getName()
                )
        );
    }

    public String createRefreshToken(User user, String jti) {
        return Ut.jwt.toString(
                refreshTokenSecret,
                refreshTokenExpireSeconds,
                Map.of(
                        "id", user.getUserId(),
                        "name", user.getName(),
                        "jti", jti
                )
        );
    }
}
