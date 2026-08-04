package com.back.domain.notification.event

data class PostLikedEvent(
    val postId: Long,
    val postOwnerId: Long,
    val actorId: Long,
)
