package com.back.domain.auth.dto

import com.back.domain.user.constant.LoginType

data class SocialUnlinkTarget(
    val userId: Long,
    val provider: LoginType,
    val providerId: String,
    val oauthRefreshToken: String?,
)
