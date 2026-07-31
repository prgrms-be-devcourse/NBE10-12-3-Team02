package com.back.domain.review.dto

import com.back.domain.review.entity.ReviewBookmark
import java.time.LocalDateTime

data class ReviewBookmarkResponse(
    val reviewId: Long,
    val concertId: Long,
    val concertName: String,
    val userName: String,
    val title: String,
    val content: String,
    val posterUrl: String?,
    val bookmarkedAt: LocalDateTime?,
) {
    companion object {
        fun from(bookmark: ReviewBookmark): ReviewBookmarkResponse {
            val review = bookmark.review
            return ReviewBookmarkResponse(
                reviewId = review.reviewId!!,
                concertId = review.concert.concertId!!,
                concertName = review.concert.concertName,
                userName = review.user.name,
                title = review.title,
                content = review.content,
                posterUrl = review.concert.urlPoster,
                bookmarkedAt = bookmark.createDate,
            )
        }
    }
}
