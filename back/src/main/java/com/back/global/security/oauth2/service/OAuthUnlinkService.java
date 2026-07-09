package com.back.global.security.oauth2.service;

import com.back.domain.user.entity.LoginType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthUnlinkService {

    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    public void unlink(LoginType loginType, String oauthRefreshToken) {
        if (oauthRefreshToken == null || oauthRefreshToken.isBlank()) {
            log.warn("OAuth Refresh Token 없음, 언링크 스킵: {}", loginType);
            return;
        }

        try {
            String accessToken = reissueAccessToken(loginType, oauthRefreshToken);
            switch (loginType) {
                case KAKAO -> unlinkKakao(accessToken);
                case NAVER -> unlinkNaver(accessToken);
                case GOOGLE -> unlinkGoogle(accessToken);
                default -> log.warn("지원하지 않는 OAuth 플랫폼: {}", loginType);
            }
        } catch (Exception e) {
            log.warn("OAuth 언링크 실패 - loginType: {}, error: {}", loginType, e.getMessage());
        }
    }

    private String reissueAccessToken(LoginType loginType, String refreshToken) {
        return switch (loginType) {
            case KAKAO -> reissueKakaoAccessToken(refreshToken);
            case NAVER -> reissueNaverAccessToken(refreshToken);
            case GOOGLE -> reissueGoogleAccessToken(refreshToken);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth 플랫폼: " + loginType);
        };
    }

    private String reissueKakaoAccessToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("refresh_token", refreshToken);

        Map response = restTemplate.postForObject(
                "https://kauth.kakao.com/oauth/token",
                new HttpEntity<>(params, headers),
                Map.class
        );
        return (String) response.get("access_token");
    }

    private String reissueNaverAccessToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", naverClientId);
        params.add("client_secret", naverClientSecret);
        params.add("refresh_token", refreshToken);

        Map response = restTemplate.postForObject(
                "https://nid.naver.com/oauth2.0/token",
                new HttpEntity<>(params, headers),
                Map.class
        );
        return (String) response.get("access_token");
    }

    private String reissueGoogleAccessToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("refresh_token", refreshToken);

        Map response = restTemplate.postForObject(
                "https://oauth2.googleapis.com/token",
                new HttpEntity<>(params, headers),
                Map.class
        );
        return (String) response.get("access_token");
    }

    private void unlinkKakao(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        restTemplate.postForObject(
                "https://kapi.kakao.com/v1/user/unlink",
                new HttpEntity<>(headers),
                String.class
        );
    }

    private void unlinkNaver(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "delete");
        params.add("client_id", naverClientId);
        params.add("client_secret", naverClientSecret);
        params.add("access_token", accessToken);

        restTemplate.postForObject(
                "https://nid.naver.com/oauth2.0/token",
                new HttpEntity<>(params, headers),
                String.class
        );
    }

    private void unlinkGoogle(String accessToken) {
        restTemplate.postForObject(
                "https://oauth2.googleapis.com/revoke?token=" + accessToken,
                HttpEntity.EMPTY,
                String.class
        );
    }
}