package com.back.domain.post.repository

interface PostLikeCountProjection {
    val postId: Long
    val likeCount: Long
}
