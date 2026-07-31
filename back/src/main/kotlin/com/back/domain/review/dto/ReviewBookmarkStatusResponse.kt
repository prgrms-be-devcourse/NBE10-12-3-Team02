package com.back.domain.review.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ReviewBookmarkStatusResponse(
    @get:JsonProperty("isBookmarked")
    val isBookmarked: Boolean,
)
