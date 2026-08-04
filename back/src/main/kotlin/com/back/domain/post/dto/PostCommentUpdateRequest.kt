package com.back.domain.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PostCommentUpdateRequest(
    @field:NotBlank(message = "댓글을 입력해주세요.")
    @field:Size(max = 1000, message = "댓글은 1000자 이내로 입력해주세요.")
    val content: String,
)
