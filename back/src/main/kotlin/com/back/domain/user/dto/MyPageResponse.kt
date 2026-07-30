package com.back.domain.user.dto

import com.back.domain.user.entity.User
import com.back.domain.user.constant.LoginType

data class MyPageResponse(
    val name: String,
    val id: String,
    val email: String,
    val loginType: String,
    val socialProvider: String?,
    val profileImageUrl: String,
    val ticketGroups: List<TicketGroupInfo>
) {
    companion object {
        fun from(
            user: User,
            socialProvider: LoginType?,
            ticketGroups: List<TicketGroupInfo>,
        ): MyPageResponse = MyPageResponse(
            name = user.name,
            id = user.loginId,
            email = user.email,
            loginType = user.loginType.name,
            socialProvider = socialProvider?.name,
            profileImageUrl = user.redirectToProfileImgUrlOrDefault,
            ticketGroups = ticketGroups
        )
    }
}
