package com.back.domain.review.controller

import com.back.domain.review.dto.ReviewCommentCreateRequest
import com.back.domain.review.dto.ReviewCommentResponse
import com.back.domain.review.service.ReviewCommentService
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
@RequestMapping("/reviews/{reviewId}/comments")
@Tag(name = "ReviewComment", description = "Review Comment API")
class ReviewCommentController(
    private val reviewCommentService: ReviewCommentService,
    private val requestContext: RequestContext
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "댓글 작성", description = "리뷰 댓글 작성 (인증 필요)")
    fun createComment(
        @PathVariable reviewId: Long,
        @RequestBody @Valid request: ReviewCommentCreateRequest
    ): RsData<ReviewCommentResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = reviewCommentService.create(reviewId, actor.id, request)
        return RsData("201-1", "댓글이 등록되었습니다.", data)
    }

    @GetMapping
    @Operation(summary = "댓글 목록 조회", description = "리뷰 댓글 목록 조회 (비인증 허용)")
    fun getComments(@PathVariable reviewId: Long): RsData<List<ReviewCommentResponse>> {
        val currentUserId = requestContext.actor?.id
        val data = reviewCommentService.getList(reviewId, currentUserId)
        return RsData("200-1", "댓글 목록 조회 성공", data)
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제", description = "본인 댓글만 삭제 가능")
    fun deleteComment(
        @PathVariable reviewId: Long,
        @PathVariable commentId: Long
    ): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        reviewCommentService.delete(commentId, actor.id)
        return RsData("200-1", "댓글이 삭제되었습니다.")
    }
}
