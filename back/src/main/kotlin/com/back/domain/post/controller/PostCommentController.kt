package com.back.domain.post.controller

import com.back.domain.post.dto.PostCommentCreateRequest
import com.back.domain.post.dto.PostCommentResponse
import com.back.domain.post.dto.PostCommentUpdateRequest
import com.back.domain.post.service.PostCommentService
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
@RequestMapping("/posts/{postId}/comments")
@Tag(name = "PostComment", description = "Post Comment API")
class PostCommentController(
    private val postCommentService: PostCommentService,
    private val requestContext: RequestContext
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "댓글 작성", description = "게시글 댓글 작성 (인증 필요)")
    fun createComment(
        @PathVariable postId: Long,
        @RequestBody @Valid request: PostCommentCreateRequest
    ): RsData<PostCommentResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = postCommentService.create(postId, actor.id, request)
        return RsData("201-1", "댓글이 등록되었습니다.", data)
    }

    @GetMapping
    @Operation(summary = "댓글 목록 조회", description = "게시글 댓글 목록 조회 (비인증 허용)")
    fun getComments(@PathVariable postId: Long): RsData<List<PostCommentResponse>> {
        val currentUserId = requestContext.actor?.id
        val data = postCommentService.getList(postId, currentUserId)
        return RsData("200-1", "댓글 목록 조회 성공", data)
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "댓글 수정", description = "본인 댓글만 수정 가능")
    fun updateComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @RequestBody @Valid request: PostCommentUpdateRequest
    ): RsData<PostCommentResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = postCommentService.update(postId, commentId, actor.id, request)
        return RsData("200-1", "댓글이 수정되었습니다.", data)
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제", description = "본인 댓글만 삭제 가능")
    fun deleteComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long
    ): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        postCommentService.delete(postId, commentId, actor.id)
        return RsData("200-1", "댓글이 삭제되었습니다.")
    }
}
