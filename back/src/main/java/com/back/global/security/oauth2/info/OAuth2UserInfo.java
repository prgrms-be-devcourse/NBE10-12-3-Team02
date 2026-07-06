package com.back.global.security.oauth2.info;

public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();
    String getName();
}