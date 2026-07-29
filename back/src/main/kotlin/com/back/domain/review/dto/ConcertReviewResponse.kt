package com.back.domain.review.dto

import com.back.domain.review.entity.ConcertReview
import java.time.LocalDateTime

data class ConcertReviewResponse(
    val reviewId: Long,
    val concertId: Long,
    val userId: Long,
    val userName: String,
    val title: String,
    val content: String,
    val rating: Int,
    val isMine: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(review: ConcertReview, currentUserId: Long?): ConcertReviewResponse =
            ConcertReviewResponse(
                reviewId = review.reviewId!!,
                concertId = review.concert.concertId!!,
                userId = review.user.userId!!,
                userName = review.user.name,
                title = review.title,
                content = review.content,
                rating = review.rating,
                isMine = currentUserId != null && review.user.userId == currentUserId,
                createdAt = review.createDate,
                updatedAt = review.modifyDate
            )
    }
}
