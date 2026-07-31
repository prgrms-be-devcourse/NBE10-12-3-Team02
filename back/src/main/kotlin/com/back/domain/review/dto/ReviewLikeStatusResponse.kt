package com.back.domain.review.dto

data class ReviewLikeStatusResponse(
    val liked: Boolean,
    val likeCount: Long,
)
