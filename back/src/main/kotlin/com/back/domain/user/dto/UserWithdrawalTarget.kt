package com.back.domain.user.dto

import com.back.domain.user.constant.LoginType

data class UserWithdrawalTarget(
    val userId: Long,
    val provider: LoginType?,
    val providerId: String?,
    val oauthRefreshToken: String?,
)
