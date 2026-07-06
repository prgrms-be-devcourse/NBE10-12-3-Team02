package com.back.global.security.oauth2.info;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getProviderId() {
        return getResponseValue("id");
    }

    public String getEmail() {
        return getResponseValue("email");
    }

    public String getName() {
        String name = getResponseValue("name");

        if (name != null && !name.isBlank()) {
            return name;
        }

        String nickname = getResponseValue("nickname");
        return nickname == null || nickname.isBlank() ? "네이버사용자" : nickname;
    }

    private Map<?, ?> getResponse() {
        Object response = attributes.get("response");

        if (response instanceof Map<?, ?> map) {
            return map;
        }

        return Map.of();
    }

    private String getResponseValue(String key) {
        Object value = getResponse().get(key);
        return value == null ? null : value.toString();
    }
}