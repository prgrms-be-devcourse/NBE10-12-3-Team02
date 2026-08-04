package com.back.domain.follow.dto

import com.back.domain.follow.entity.Follow
import java.time.LocalDateTime

data class FollowUserResponse(
    val userId: Long,
    val name: String,
    val profileImgUrl: String,
    val followedAt: LocalDateTime?,
) {
    companion object {
        fun ofFollowee(follow: Follow): FollowUserResponse {
            val user = follow.followee
            return FollowUserResponse(
                userId = user.userIdOrThrow,
                name = user.name,
                profileImgUrl = user.profileImgUrlOrDefault,
                followedAt = follow.createDate,
            )
        }

        fun ofFollower(follow: Follow): FollowUserResponse {
            val user = follow.follower
            return FollowUserResponse(
                userId = user.userIdOrThrow,
                name = user.name,
                profileImgUrl = user.profileImgUrlOrDefault,
                followedAt = follow.createDate,
            )
        }
    }
}
