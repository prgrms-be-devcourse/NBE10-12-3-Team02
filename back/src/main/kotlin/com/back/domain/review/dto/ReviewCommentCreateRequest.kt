package com.back.domain.review.dto

import jakarta.validation.constraints.NotBlank

data class ReviewCommentCreateRequest(
    @field:NotBlank val content: String
)
