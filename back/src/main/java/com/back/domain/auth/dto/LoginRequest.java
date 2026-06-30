package com.back.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest (
    @NotBlank String id,
    @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") String password
) {}
