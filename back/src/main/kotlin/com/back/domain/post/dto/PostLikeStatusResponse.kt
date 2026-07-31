package com.back.domain.post.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PostLikeStatusResponse(
    @get:JsonProperty("isLiked")
    val isLiked: Boolean,
    val likeCount: Long,
)
