package com.back.domain.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SignupRequest(
    @field:NotBlank(message = "아이디를 입력해주세요.")
    val id: String? = null,

    @field:NotBlank(message = "이메일을 입력해주세요.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String? = null,

    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    val password: String? = null,

    @field:NotBlank(message = "이름을 입력해주세요.")
    @field:Pattern(regexp = "^\\S+$", message = "이름에 공백을 포함할 수 없습니다.")
    val name: String? = null,

    @field:NotBlank(message = "이메일 인증 토큰이 필요합니다.")
    val verificationToken: String? = null,
)
