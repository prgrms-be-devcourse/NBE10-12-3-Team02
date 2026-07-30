package com.back.domain.auth.dto

data class SocialLinkStartResult(
    val intentId: String,
    val authorizationPath: String,
)
