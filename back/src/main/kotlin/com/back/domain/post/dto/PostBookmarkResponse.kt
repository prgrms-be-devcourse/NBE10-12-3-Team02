package com.back.domain.post.dto

import com.back.domain.post.entity.PostBookmark
import java.time.LocalDateTime

data class PostBookmarkResponse(
    val postId: Long,
    val concertId: Long,
    val concertName: String,
    val userName: String,
    val title: String,
    val content: String,
    val posterUrl: String?,
    val bookmarkedAt: LocalDateTime?,
) {
    companion object {
        fun from(bookmark: PostBookmark): PostBookmarkResponse {
            val post = bookmark.post
            return PostBookmarkResponse(
                postId = post.postIdOrThrow,
                concertId = post.concert.concertIdOrThrow,
                concertName = post.concert.concertName,
                userName = post.user.name,
                title = post.title,
                content = post.content,
                posterUrl = post.concert.urlPoster,
                bookmarkedAt = bookmark.createDate,
            )
        }
    }
}
