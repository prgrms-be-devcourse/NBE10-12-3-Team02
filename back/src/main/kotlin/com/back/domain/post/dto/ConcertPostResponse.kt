package com.back.domain.post.dto

import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.ReviewType
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ConcertPostResponse(
    val postId: Long,
    val concertId: Long,
    val userId: Long,
    val userName: String,
    val title: String,
    val content: String,
    val rating: Int?,
    val reviewType: ReviewType,
    val summary: String?,
    @get:JsonProperty("isMine") val isMine: Boolean,
    val likeCount: Long,
    @get:JsonProperty("isLiked") val isLiked: Boolean,
    @get:JsonProperty("isBookmarked") val isBookmarked: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val concertName: String,
    val posterUrl: String?
) {
    companion object {
        fun of(
            post: ConcertPost,
            currentUserId: Long?,
            likeCount: Long = 0L,
            isLiked: Boolean = false,
            isBookmarked: Boolean = false,
        ): ConcertPostResponse =
            ConcertPostResponse(
                postId = post.postIdOrThrow,
                concertId = post.concert.concertIdOrThrow,
                userId = post.user.userIdOrThrow,
                userName = post.user.name,
                title = post.title,
                content = post.content,
                rating = post.rating,
                reviewType = post.reviewType,
                summary = post.summary,
                isMine = currentUserId != null && post.user.userId == currentUserId,
                likeCount = likeCount,
                isLiked = isLiked,
                isBookmarked = isBookmarked,
                createdAt = post.createDate,
                updatedAt = post.modifyDate,
                concertName = post.concert.concertName,
                posterUrl = post.concert.urlPoster
            )
    }
}
