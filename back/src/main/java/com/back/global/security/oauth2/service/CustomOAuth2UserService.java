package com.back.global.security.oauth2.service;

import com.back.domain.user.entity.LoginType;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.security.oauth2.info.GoogleOAuth2UserInfo;
import com.back.global.security.oauth2.info.KakaoOAuth2UserInfo;
import com.back.global.security.oauth2.info.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        String refreshToken = userRequest.getAdditionalParameters()
                .getOrDefault("refresh_token", "").toString();

        log.info("OAuth2 additionalParameters: {}", userRequest.getAdditionalParameters());
        log.info("OAuth2 refreshToken: {}", refreshToken);

        User user = switch (registrationId) {
            case "kakao" -> getOrCreateUser(oAuth2User.getAttributes(), LoginType.KAKAO, refreshToken);
            case "google" -> getOrCreateUser(oAuth2User.getAttributes(), LoginType.GOOGLE, refreshToken);
            default -> throw new OAuth2AuthenticationException("oauth2_provider_not_supported");
        };

        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                Map.of(
                        "userId", user.getUserId(),
                        "loginId", user.getLoginId(),
                        "email", user.getEmail(),
                        "name", user.getName()
                ),
                "userId"
        );
    }

    private OAuth2UserInfo createUserInfo(
            Map<String, Object> attributes,
            LoginType loginType
    ) {
        return switch (loginType) {
            case KAKAO -> new KakaoOAuth2UserInfo(attributes);
            case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("oauth2_provider_not_supported");
        };
    }

    private User getOrCreateUser(Map<String, Object> attributes, LoginType loginType, String refreshToken) {
        OAuth2UserInfo userInfo = createUserInfo(attributes, loginType);

        String platformId = userInfo.getProviderId();
        String loginId = loginType.name() + "_" + platformId;
        String email = userInfo.getEmail();
        String name = userInfo.getName();

        validateRequired(platformId, "oauth2_provider_id_missing");
        validateRequired(email, "oauth2_email_missing");

        return userRepository.findByLoginIdAndDeletedAtIsNull(loginId)
                .map(user -> {
                    if (!refreshToken.isBlank()) {
                        user.updateOauthRefreshToken(refreshToken);
                    }
                    return user;
                })
                .orElseGet(() -> createOAuthUser(loginId, email, name, loginType, refreshToken));
    }

    private User createOAuthUser(
            String loginId,
            String email,
            String name,
            LoginType loginType,
            String refreshToken
    ) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new OAuth2AuthenticationException("oauth2_email_already_exists");
        }

        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = User.createOAuth(
                loginId,
                email,
                randomPassword,
                name,
                loginType,
                refreshToken.isBlank() ? null : refreshToken
        );

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new OAuth2AuthenticationException("oauth2_email_already_exists");
        }
    }

    private void validateRequired(String value, String errorCode) {
        if (value == null || value.isBlank()) {
            throw new OAuth2AuthenticationException(errorCode);
        }
    }
}