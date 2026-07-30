package com.back.domain.review.dto

import com.back.domain.review.entity.ReviewComment
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ReviewCommentResponse(
    val commentId: Long,
    val authorName: String,
    val content: String,
    val createdAt: LocalDateTime?,
    @get:JsonProperty("isMine") val isMine: Boolean
) {
    companion object {
        fun of(comment: ReviewComment, currentUserId: Long?): ReviewCommentResponse =
            ReviewCommentResponse(
                commentId = comment.commentId!!,
                authorName = comment.user.name,
                content = comment.content,
                createdAt = comment.createDate,
                isMine = currentUserId != null && comment.user.userId == currentUserId
            )
    }
}
