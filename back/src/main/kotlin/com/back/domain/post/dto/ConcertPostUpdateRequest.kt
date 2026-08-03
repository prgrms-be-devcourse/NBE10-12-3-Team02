package com.back.domain.post.dto

import com.back.domain.post.entity.ReviewType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class ConcertPostUpdateRequest(
    @field:NotBlank(message = "제목을 입력해주세요.")
    @field:Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    val title: String,

    @field:NotBlank(message = "내용을 입력해주세요.")
    @field:Size(max = 2000, message = "내용은 2000자 이내로 입력해주세요.")
    val content: String,

    @field:NotNull(message = "게시글 유형을 선택해주세요.")
    val reviewType: ReviewType,

    @field:Min(value = 1, message = "평점은 1 이상이어야 합니다.")
    @field:Max(value = 5, message = "평점은 5 이하여야 합니다.")
    val rating: Int?
)
