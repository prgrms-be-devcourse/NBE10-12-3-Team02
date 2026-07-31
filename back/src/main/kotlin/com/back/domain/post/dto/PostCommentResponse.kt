package com.back.domain.post.dto

import com.back.domain.post.entity.PostComment
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class PostCommentResponse(
    val commentId: Long,
    val authorName: String,
    val content: String,
    val createdAt: LocalDateTime?,
    @get:JsonProperty("isMine") val isMine: Boolean
) {
    companion object {
        fun of(comment: PostComment, currentUserId: Long?): PostCommentResponse =
            PostCommentResponse(
                commentId = comment.commentId!!,
                authorName = comment.user.name,
                content = comment.content,
                createdAt = comment.createDate,
                isMine = currentUserId != null && comment.user.userId == currentUserId
            )
    }
}
