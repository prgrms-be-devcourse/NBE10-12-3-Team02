package com.back.global.security.oauth2.info;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        Object sub = attributes.get("sub");
        return sub == null ? null : sub.toString();
    }

    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        return email == null ? null : email.toString();
    }

    @Override
    public String getName() {
        Object name = attributes.get("name");
        return name == null ? "구글사용자" : name.toString();
    }
}