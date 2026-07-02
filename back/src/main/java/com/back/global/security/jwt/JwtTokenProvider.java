package com.back.global.security.jwt;

import com.back.domain.user.entity.User;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.security.jwt.payload.AccessTokenPayload;
import com.back.global.security.jwt.payload.RefreshTokenPayload;
import com.back.domain.auth.util.Ut;
import io.jsonwebtoken.ExpiredJwtException;
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
        try {
            Map<String, Object> payload = Ut.jwt.payload(refreshTokenSecret, refreshToken);

            Long userId = getLongClaim(payload, "id");
            String jti = getStringClaim(payload, "jti");

            return new RefreshTokenPayload(userId, jti);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public AccessTokenPayload parseAccessToken(String accessToken) {
        try {
            Map<String, Object> payload = Ut.jwt.payload(accessTokenSecret, accessToken);

            Long userId = getLongClaim(payload, "id");
            String name = getStringClaim(payload, "name");

            return new AccessTokenPayload(userId, name);
        } catch (ExpiredJwtException e) {
            throw new ServiceException(ErrorCode.AUTH_EXPIRED_ACCESS_TOKEN);
        } catch (RuntimeException e) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_ACCESS_TOKEN);
        }
    }

    public long getRemainingSeconds(String accessToken) {
        Map<String, Object> payload = Ut.jwt.payload(accessTokenSecret, accessToken);
        if (payload == null) return 0;

        Object exp = payload.get("exp");
        if (exp == null) return 0;

        long expTime = ((Number) exp).longValue();
        long now = System.currentTimeMillis() / 1000;
        return Math.max(0, expTime - now);
    }

    private Long getLongClaim(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        if (value == null) {
            throw new IllegalArgumentException("Missing claim: " + "id");
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.valueOf(value.toString());
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