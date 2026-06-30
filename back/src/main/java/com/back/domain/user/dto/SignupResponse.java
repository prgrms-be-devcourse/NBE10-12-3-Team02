package com.back.domain.user.dto;

import com.back.domain.user.entity.User;

public record SignupResponse(
        Long userId,
        String loginType
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(user.getUserId(), user.getLoginType().name());
    }
}