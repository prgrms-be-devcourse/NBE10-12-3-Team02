package com.back.domain.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty

// 프론트(front/src/lib/api.ts AuthRestoreResponse)가 "authenticated" 키를 기대하므로 맞춰준다.
data class AuthRestoreResponse(
    @get:JsonProperty("authenticated") val isLogin: Boolean
)
