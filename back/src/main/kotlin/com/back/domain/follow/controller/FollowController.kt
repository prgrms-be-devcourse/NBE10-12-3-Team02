package com.back.domain.follow.controller

import com.back.domain.follow.dto.FollowStatusResponse
import com.back.domain.follow.dto.FollowUserResponse
import com.back.domain.follow.service.FollowService
import com.back.global.annotation.ApiV1
import com.back.global.requestcontext.RequestContext
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@ApiV1
@RestController
@RequestMapping("/follows")
@Tag(name = "Follow", description = "Follow API")
class FollowController(
    private val followService: FollowService,
    private val requestContext: RequestContext
) {

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "팔로우", description = "특정 유저를 팔로우합니다.")
    fun follow(@PathVariable userId: Long): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        followService.follow(actor.id, userId)
        return RsData("201-1", "팔로우했습니다.")
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "언팔로우", description = "특정 유저에 대한 팔로우를 취소합니다.")
    fun unfollow(@PathVariable userId: Long): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        followService.unfollow(actor.id, userId)
        return RsData("200-1", "언팔로우했습니다.")
    }

    @GetMapping("/{userId}/status")
    @Operation(summary = "팔로우 상태 조회", description = "특정 유저에 대한 팔로우 여부를 조회합니다.")
    fun getStatus(@PathVariable userId: Long): RsData<FollowStatusResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = followService.isFollowing(actor.id, userId)
        return RsData("200-1", "팔로우 상태 조회 성공", data)
    }

    @GetMapping("/me")
    @Operation(summary = "팔로잉 목록 조회", description = "내가 팔로우 중인 유저 목록을 조회합니다.")
    fun getMyFollowings(
        @PageableDefault(size = 20) pageable: Pageable,
    ): RsData<Page<FollowUserResponse>> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = followService.getFollowings(actor.id, pageable)
        return RsData("200-1", "팔로잉 목록 조회 성공", data)
    }

    @GetMapping("/me/followers")
    @Operation(summary = "팔로워 목록 조회", description = "나를 팔로우 중인 유저 목록을 조회합니다.")
    fun getMyFollowers(
        @PageableDefault(size = 20) pageable: Pageable,
    ): RsData<Page<FollowUserResponse>> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = followService.getFollowers(actor.id, pageable)
        return RsData("200-1", "팔로워 목록 조회 성공", data)
    }
}
