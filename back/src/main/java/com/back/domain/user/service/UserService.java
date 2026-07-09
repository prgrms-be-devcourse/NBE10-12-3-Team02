package com.back.domain.user.service;

import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.dto.*;
import com.back.domain.user.entity.LoginType;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.security.filter.BearerTokenExtractor;
import com.back.global.security.jwt.repository.BlacklistRepository;
import com.back.global.security.jwt.JwtTokenProvider;
import com.back.global.security.jwt.repository.RefreshTokenRepository;
import com.back.global.security.oauth2.service.OAuthUnlinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistRepository blacklistRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BearerTokenExtractor bearerTokenExtractor;
    private final OAuthUnlinkService oAuthUnlinkService;

    @Value("${custom.jwt.blacklist.grace-seconds}")
    private long tokenBlacklistGraceSeconds;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByLoginIdAndDeletedAtIsNull(request.id())) {
            throw new ServiceException(ErrorCode.USER_ID_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new ServiceException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }
        User user = userRepository.save(
                User.create(request.id(), request.email(),
                        passwordEncoder.encode(request.password()),
                        request.name(), LoginType.NORMAL)
        );
        return SignupResponse.from(user);
    }

    @Transactional
    public void withdraw(Long userId, String authorization) {
        String accessToken = bearerTokenExtractor.extract(authorization);
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND_OR_DELETED));

        if (user.getLoginType() != LoginType.NORMAL) {
            oAuthUnlinkService.unlink(user.getLoginType(), user.getOauthRefreshToken());
        }

        user.withdraw();
        refreshTokenRepository.deleteAllByUserId(userId);
        long remaining = jwtTokenProvider.getRemainingSeconds(accessToken);
        blacklistRepository.add(accessToken, Duration.ofSeconds(remaining + tokenBlacklistGraceSeconds));
    }

    public MyPageResponse getMyPage(Long userId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        List<TicketGroupInfo> ticketGroups = ticketRepository.findAllByUserWithConcert(user)
                .stream()
                .collect(Collectors.groupingBy(t -> t.getSchedule().getScheduleId()))
                .values()
                .stream()
                .map(TicketGroupInfo::from)
                .toList();

        return MyPageResponse.from(user, ticketGroups);
    }

    @Transactional
    public void updateMyPage(Long userId, UpdateMyPageRequest request) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (request.name() != null) {
            String trimmed = request.name().trim();
            if (trimmed.isEmpty() || trimmed.contains(" ")) {
                throw new ServiceException(ErrorCode.USER_NAME_INVALID);
            }
            user.updateName(trimmed);
        }

        if (request.email() != null) {
            if (!user.getEmail().equals(request.email())
                    && userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
                throw new ServiceException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
            }
            user.updateEmail(request.email());
        }

        if (request.password() != null) {
            user.updatePassword(passwordEncoder.encode(request.password()));
        }
    }

    public void checkId(String id) {
        if (userRepository.existsByLoginIdAndDeletedAtIsNull(id)) {
            throw new ServiceException(ErrorCode.USER_ID_ALREADY_EXISTS);
        }
    }
}