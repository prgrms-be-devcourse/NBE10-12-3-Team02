package com.back.domain.auth.service;

import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.security.RefreshTokenRepository;
import com.back.global.security.TokenHashUtil;
import com.back.standard.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    @Value("${custom.jwt.accessToken.secret}")
    private String accessTokenSecret;

    @Value("${custom.jwt.refreshToken.secret}")
    private String refreshTokenSecret;

    @Value("${custom.jwt.accessToken.expirationSeconds}")
    private int accessTokenExpireSeconds;

    @Value("${custom.jwt.refreshToken.expirationSeconds}")
    private int refreshTokenExpireSeconds;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public record TokenResponse(
            String accessToken,
            String refreshToken
    ) {
    }

    public TokenResponse login(User user) {
        String accessToken = createAccessToken(user);

        String refreshTokenJti = UUID.randomUUID().toString();
        String refreshToken = createRefreshToken(user, refreshTokenJti);

        String refreshTokenHash = TokenHashUtil.sha256(refreshToken);

        refreshTokenRepository.save(
                user.getUserId(),
                refreshTokenJti,
                refreshTokenHash,
                Duration.ofSeconds(refreshTokenExpireSeconds)
        );

        return new TokenResponse(accessToken, refreshToken);
    }

    public void logout(String refreshToken) {
        if (!Ut.jwt.isValid(refreshTokenSecret, refreshToken)) {
            return;
        }

        Map<String, Object> payload = Ut.jwt.payload(refreshTokenSecret, refreshToken);

        Long memberId = Long.valueOf(payload.get("id").toString());
        String jti = payload.get("jti").toString();

        refreshTokenRepository.delete(memberId, jti);
    }

    public User findUser(String refreshToken) {
        if (!Ut.jwt.isValid(refreshTokenSecret, refreshToken)) {
            return null;
        }

        Map<String, Object> payload = Ut.jwt.payload(refreshTokenSecret, refreshToken);

        Long userId = Long.valueOf(payload.get("id").toString());
        return userRepository.findById(userId).orElse(null);
    }

    public TokenResponse refresh(String refreshToken) {
        if (!Ut.jwt.isValid(refreshTokenSecret, refreshToken)) {
            throw new ServiceException(
                    ErrorCode.AUTH_INVALID_REFRESH_TOKEN
            );
        }

        Map<String, Object> payload = Ut.jwt.payload(refreshTokenSecret, refreshToken);

        Long userId = Long.valueOf(payload.get("id").toString());
        String oldJti = payload.get("jti").toString();

        String savedRefreshTokenHash = refreshTokenRepository.find(userId, oldJti);

        if (savedRefreshTokenHash == null) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        String requestRefreshTokenHash = TokenHashUtil.sha256(refreshToken);

        if (!savedRefreshTokenHash.equals(requestRefreshTokenHash)) {
            throw new ServiceException(ErrorCode.AUTH_REFRESH_TOKEN_MISMATCH);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.delete(userId, oldJti);

        String newAccessToken = createAccessToken(user);

        String newJti = UUID.randomUUID().toString();
        String newRefreshToken = createRefreshToken(user, newJti);

        refreshTokenRepository.save(
                userId,
                newJti,
                TokenHashUtil.sha256(newRefreshToken),
                Duration.ofSeconds(refreshTokenExpireSeconds)
        );

        return new TokenResponse(newAccessToken, newRefreshToken);
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


    public Map<String, Object> payload(String accessToken) {
        Map<String, Object> parsedPayload = Ut.jwt.payload(accessTokenSecret, accessToken);

        if (parsedPayload == null) return null;

        Long id = ((Number) parsedPayload.get("id")).longValue();
        String name = (String) parsedPayload.get("name");

        return Map.of("id", id, "name", name);
    }

    public void checkPassword(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new ServiceException(ErrorCode.AUTH_PASSWORD_MISMATCH);
    }

    public User findByLoginId(String id) {
        return userRepository.findByLoginIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
    }
}