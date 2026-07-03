package com.back.domain.auth.service;

import com.back.domain.auth.dto.TokenResponse;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.security.filter.BearerTokenExtractor;
import com.back.global.security.jwt.*;
import com.back.global.security.jwt.payload.RefreshTokenPayload;
import com.back.global.security.jwt.repository.BlacklistRepository;
import com.back.global.security.jwt.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistRepository blacklistRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final BearerTokenExtractor bearerTokenExtractor;

    @Value("${custom.jwt.refreshToken.expirationSeconds}")
    private int refreshTokenExpireSeconds;

    public TokenResponse login(String id, String password) {
        User user = userRepository.findByLoginIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new ServiceException(ErrorCode.AUTH_PASSWORD_MISMATCH);

        String accessToken = jwtTokenProvider.createAccessToken(user);

        String refreshTokenJti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(user, refreshTokenJti);

        String refreshTokenHash = TokenHashUtil.sha256(refreshToken);

        refreshTokenRepository.save(
                user.getUserId(),
                refreshTokenJti,
                refreshTokenHash,
                Duration.ofSeconds(refreshTokenExpireSeconds)
        );

        return new TokenResponse(accessToken, refreshToken);
    }

    public void logout(String refreshToken, String authorization) {
        deleteRefreshTokenIfValid(refreshToken);
        blacklistAccessTokenIfValid(authorization);
    }

    public TokenResponse refresh(String refreshToken) {
        RefreshTokenPayload payload = jwtTokenProvider.parseRefreshToken(refreshToken);

        if (payload == null) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByUserIdAndDeletedAtIsNull(payload.userId())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        String requestRefreshTokenHash = TokenHashUtil.sha256(refreshToken);

        String newAccessToken = jwtTokenProvider.createAccessToken(user);

        String newJti = UUID.randomUUID().toString();
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user, newJti);
        String newRefreshTokenHash = TokenHashUtil.sha256(newRefreshToken);

        RefreshTokenRotateResult rotateResult = refreshTokenRepository.rotate(
                payload.userId(),
                payload.jti(),
                requestRefreshTokenHash,
                newJti,
                newRefreshTokenHash,
                Duration.ofSeconds(refreshTokenExpireSeconds)
        );

        handleRotateFailure(rotateResult, payload.userId());

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    private void handleRotateFailure(RefreshTokenRotateResult rotateResult, Long userId) {
        if (rotateResult == RefreshTokenRotateResult.SUCCESS) {
            return;
        }

        if (rotateResult == RefreshTokenRotateResult.MISMATCH) {
            refreshTokenRepository.deleteAllByUserId(userId);
            throw new ServiceException(ErrorCode.AUTH_REFRESH_TOKEN_MISMATCH);
        }

        if (rotateResult == RefreshTokenRotateResult.NOT_FOUND) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        throw new ServiceException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    private void deleteRefreshTokenIfValid(String refreshToken) {
        RefreshTokenPayload payload = jwtTokenProvider.parseRefreshToken(refreshToken);

        if (payload != null) {
            refreshTokenRepository.delete(payload.userId(), payload.jti());
        }
    }

    private void blacklistAccessTokenIfValid(String authorization) {
        String accessToken = bearerTokenExtractor.extractAccessTokenOrNull(authorization);

        if (accessToken == null) {
            return;
        }

        try {
            long remaining = jwtTokenProvider.getRemainingSeconds(accessToken);

            if (remaining > 0) {
                blacklistRepository.add(accessToken, Duration.ofSeconds(remaining + 60));
            }
        } catch (RuntimeException ignored) {
        }
    }
}
