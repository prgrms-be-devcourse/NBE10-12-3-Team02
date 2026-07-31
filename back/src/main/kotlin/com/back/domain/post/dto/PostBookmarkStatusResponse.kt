package com.back.domain.post.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PostBookmarkStatusResponse(
    @get:JsonProperty("isBookmarked")
    val isBookmarked: Boolean,
)
