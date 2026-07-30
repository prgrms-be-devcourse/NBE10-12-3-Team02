package com.back.domain.auth.dto

import com.back.domain.user.entity.User

data class SocialLinkStatusResponse(
    val linked: Boolean,
    val provider: String?,
) {
    companion object {
        fun from(user: User): SocialLinkStatusResponse =
            SocialLinkStatusResponse(
                linked = user.socialProvider != null,
                provider = user.socialProvider?.name,
            )
    }
}
