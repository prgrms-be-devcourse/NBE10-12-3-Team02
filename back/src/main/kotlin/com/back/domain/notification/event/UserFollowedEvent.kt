package com.back.domain.notification.event

data class UserFollowedEvent(
    val followeeId: Long,
    val followerId: Long,
)
