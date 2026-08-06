package com.back.domain.follow.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class FollowStatusResponse(
    @get:JsonProperty("isFollowing") val isFollowing: Boolean,
)
