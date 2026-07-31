package com.back.domain.review.dto

import com.back.domain.review.entity.ConcertReview
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ConcertReviewResponse(
    val reviewId: Long,
    val concertId: Long,
    val userId: Long,
    val userName: String,
    val title: String,
    val content: String,
    @get:JsonProperty("isMine") val isMine: Boolean,
    val likeCount: Long,
    @get:JsonProperty("isLiked") val isLiked: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val concertName: String,
    val posterUrl: String?
) {
    companion object {
        fun of(
            review: ConcertReview,
            currentUserId: Long?,
            likeCount: Long = 0L,
            isLiked: Boolean = false,
        ): ConcertReviewResponse =
            ConcertReviewResponse(
                reviewId = review.reviewId!!,
                concertId = review.concert.concertId!!,
                userId = review.user.userId!!,
                userName = review.user.name,
                title = review.title,
                content = review.content,
                isMine = currentUserId != null && review.user.userId == currentUserId,
                likeCount = likeCount,
                isLiked = isLiked,
                createdAt = review.createDate,
                updatedAt = review.modifyDate,
                concertName = review.concert.concertName,
                posterUrl = review.concert.urlPoster
            )
    }
}
