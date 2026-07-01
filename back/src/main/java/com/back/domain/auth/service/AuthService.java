package com.back.domain.auth.service;

import com.back.domain.auth.dto.TokenResponse;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.security.jwt.JwtTokenProvider;
import com.back.global.security.jwt.payload.RefreshTokenPayload;
import com.back.global.security.jwt.RefreshTokenRepository;
import com.back.global.security.jwt.TokenHashUtil;
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
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${custom.jwt.refreshToken.expirationSeconds}")
    private int refreshTokenExpireSeconds;

    public TokenResponse login(String id, String password) {
        User user = userRepository.findByLoginIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new ServiceException(ErrorCode.AUTH_PASSWORD_MISMATCH);

        refreshTokenRepository.deleteAllByUserId(user.getUserId());

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

    public void logout(String refreshToken) {
        RefreshTokenPayload payload = jwtTokenProvider.parseRefreshToken(refreshToken);
        if (payload == null) return;

        refreshTokenRepository.delete(payload.userId(), payload.jti());
    }

    public TokenResponse refresh(String refreshToken) {
        RefreshTokenPayload payload = jwtTokenProvider.parseRefreshToken(refreshToken);

        if (payload == null) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        };

        String savedRefreshTokenHash = refreshTokenRepository.find(payload.userId(), payload.jti());

        if (savedRefreshTokenHash == null) { // 구 리프레시 토큰 재사용의 경우
            refreshTokenRepository.deleteAllByUserId(payload.userId());
            throw new ServiceException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        String requestRefreshTokenHash = TokenHashUtil.sha256(refreshToken);

        if (!savedRefreshTokenHash.equals(requestRefreshTokenHash)) {
            refreshTokenRepository.deleteAllByUserId(payload.userId());
            throw new ServiceException(ErrorCode.AUTH_REFRESH_TOKEN_MISMATCH);
        }

        User user = userRepository.findById(payload.userId())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.delete(payload.userId(), payload.jti());

        String newAccessToken = jwtTokenProvider.createAccessToken(user);

        String newJti = UUID.randomUUID().toString();
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user, newJti);

        refreshTokenRepository.save(
                payload.userId(),
                newJti,
                TokenHashUtil.sha256(newRefreshToken),
                Duration.ofSeconds(refreshTokenExpireSeconds)
        );

        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}
