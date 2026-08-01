package com.back.domain.user.dto

import com.back.domain.user.entity.User

data class UserProfileResponse(
    val userId: Long,
    val name: String,
    val profileImgUrl: String,
    val isFollowing: Boolean?,
) {
    companion object {
        fun from(user: User, isFollowing: Boolean?): UserProfileResponse = UserProfileResponse(
            userId = user.userId!!,
            name = user.name,
            profileImgUrl = user.profileImgUrlOrDefault,
            isFollowing = isFollowing,
        )
    }
}
