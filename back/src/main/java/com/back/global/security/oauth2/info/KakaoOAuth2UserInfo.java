package com.back.global.security.oauth2.info;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        Object id = attributes.get("id");
        return id == null ? null : id.toString();
    }

    @Override
    public String getEmail() {
        Object email = getKakaoAccount().get("email");
        return email == null ? null : email.toString();
    }

    @Override
    public String getName() {
        Object profileObject = getKakaoAccount().get("profile");

        if (!(profileObject instanceof Map<?, ?> profile)) {
            return "카카오사용자";
        }

        Object nickname = profile.get("nickname");
        return nickname == null ? "카카오사용자" : nickname.toString();
    }

    private Map<?, ?> getKakaoAccount() {
        Object kakaoAccount = attributes.get("kakao_account");

        if (kakaoAccount instanceof Map<?, ?> map) {
            return map;
        }

        return Map.of();
    }
}