package com.back.domain.user.dto

import com.back.domain.user.entity.User
import com.fasterxml.jackson.annotation.JsonProperty

data class UserProfileResponse(
    val userId: Long,
    val name: String,
    val profileImgUrl: String,
    @get:JsonProperty("isFollowing") val isFollowing: Boolean?,
) {
    companion object {
        fun from(user: User, isFollowing: Boolean?): UserProfileResponse = UserProfileResponse(
            userId = user.userIdOrThrow,
            name = user.name,
            profileImgUrl = user.profileImgUrlOrDefault,
            isFollowing = isFollowing,
        )
    }
}
