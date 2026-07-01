package com.back.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateMyPageRequest(
        String name,
        @Email String email,
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") String password
) {}