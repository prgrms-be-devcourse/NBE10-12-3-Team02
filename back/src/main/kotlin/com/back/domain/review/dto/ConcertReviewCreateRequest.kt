package com.back.domain.review.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ConcertReviewCreateRequest(
    @field:NotBlank(message = "제목을 입력해주세요.")
    @field:Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    val title: String,

    @field:NotBlank(message = "내용을 입력해주세요.")
    @field:Size(max = 2000, message = "내용은 2000자 이내로 입력해주세요.")
    val content: String,

    @field:Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @field:Max(value = 5, message = "별점은 5점 이하이어야 합니다.")
    val rating: Int
)
