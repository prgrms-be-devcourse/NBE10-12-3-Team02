package com.back.domain.post.dto

import com.back.domain.post.entity.PostLike
import java.time.LocalDateTime

data class PostLikeResponse(
    val postId: Long,
    val concertId: Long,
    val concertName: String,
    val userName: String,
    val title: String,
    val content: String,
    val posterUrl: String?,
    val likedAt: LocalDateTime?,
) {
    companion object {
        fun from(like: PostLike): PostLikeResponse {
            val post = like.post
            return PostLikeResponse(
                postId = post.postIdOrThrow,
                concertId = post.concert.concertIdOrThrow,
                concertName = post.concert.concertName,
                userName = post.user.name,
                title = post.title,
                content = post.content,
                posterUrl = post.concert.urlPoster,
                likedAt = like.createDate,
            )
        }
    }
}
