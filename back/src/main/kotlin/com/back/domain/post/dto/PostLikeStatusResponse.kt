package com.back.domain.post.dto

data class PostLikeStatusResponse(
    val liked: Boolean,
    val likeCount: Long,
)
