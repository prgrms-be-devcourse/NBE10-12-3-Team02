package com.back.domain.post.controller

import com.back.domain.post.dto.ConcertPostCreateRequest
import com.back.domain.post.dto.ConcertPostResponse
import com.back.domain.post.dto.ConcertPostUpdateRequest
import com.back.domain.post.service.ConcertPostService
import com.back.global.annotation.ApiV1
import com.back.global.requestcontext.RequestContext
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@ApiV1
@RestController
@RequestMapping("/concerts/{concertId}/posts")
@Tag(name = "ConcertPost", description = "Concert Post API")
class ConcertPostController(
    private val concertPostService: ConcertPostService,
    private val requestContext: RequestContext
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "게시글 작성", description = "콘서트 게시글 작성 API")
    fun createPost(
        @PathVariable concertId: Long,
        @RequestBody @Valid request: ConcertPostCreateRequest
    ): RsData<ConcertPostResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = concertPostService.create(concertId, actor.id, request)
        return RsData("201-1", "게시글이 작성되었습니다.", data)
    }

    @GetMapping
    @Operation(summary = "게시글 목록 조회", description = "콘서트 게시글 목록 조회 API")
    fun getPosts(
        @PathVariable concertId: Long
    ): RsData<List<ConcertPostResponse>> {
        val currentUserId = requestContext.actor?.id
        val data = concertPostService.getList(concertId, currentUserId)
        return RsData("200-1", "게시글 목록 조회 성공", data)
    }

    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회", description = "콘서트 게시글 상세 조회 API")
    fun getPost(
        @PathVariable concertId: Long,
        @PathVariable postId: Long
    ): RsData<ConcertPostResponse> {
        val currentUserId = requestContext.actor?.id
        val data = concertPostService.getDetail(concertId, postId, currentUserId)
        return RsData("200-1", "게시글 상세 조회 성공", data)
    }

    @PutMapping("/{postId}")
    @Operation(summary = "게시글 수정", description = "콘서트 게시글 수정 API")
    fun updatePost(
        @PathVariable concertId: Long,
        @PathVariable postId: Long,
        @RequestBody @Valid request: ConcertPostUpdateRequest
    ): RsData<ConcertPostResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = concertPostService.update(concertId, postId, actor.id, request)
        return RsData("200-1", "게시글이 수정되었습니다.", data)
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제", description = "콘서트 게시글 삭제 API")
    fun deletePost(
        @PathVariable concertId: Long,
        @PathVariable postId: Long
    ): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        concertPostService.delete(concertId, postId, actor.id)
        return RsData("200-1", "게시글이 삭제되었습니다.")
    }
}
