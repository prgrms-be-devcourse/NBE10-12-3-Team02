package com.back.domain.auth.dto

import com.back.domain.user.constant.LoginType

data class SocialLinkStatusResponse(
    val linked: Boolean,
    val provider: String?,
) {
    companion object {
        fun from(provider: LoginType?): SocialLinkStatusResponse =
            SocialLinkStatusResponse(
                linked = provider != null,
                provider = provider?.name,
            )
    }
}
