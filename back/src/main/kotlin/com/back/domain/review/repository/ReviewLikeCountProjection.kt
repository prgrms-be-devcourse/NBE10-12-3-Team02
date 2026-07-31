package com.back.domain.review.repository

interface ReviewLikeCountProjection {
    val reviewId: Long
    val likeCount: Long
}
