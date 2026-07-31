package com.back.domain.review.controller

import com.back.domain.review.dto.ConcertReviewCreateRequest
import com.back.domain.review.dto.ConcertReviewResponse
import com.back.domain.review.dto.ConcertReviewUpdateRequest
import com.back.domain.review.service.ConcertReviewService
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
@RequestMapping("/concerts/{concertId}/reviews")
@Tag(name = "ConcertReview", description = "Concert Review API")
class ConcertReviewController(
    private val concertReviewService: ConcertReviewService,
    private val requestContext: RequestContext
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "리뷰 작성", description = "콘서트 리뷰 작성 API")
    fun createReview(
        @PathVariable concertId: Long,
        @RequestBody @Valid request: ConcertReviewCreateRequest
    ): RsData<ConcertReviewResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = concertReviewService.create(concertId, actor.id, request)
        return RsData("201-1", "리뷰가 작성되었습니다.", data)
    }

    @GetMapping
    @Operation(summary = "리뷰 목록 조회", description = "콘서트 리뷰 목록 조회 API")
    fun getReviews(
        @PathVariable concertId: Long
    ): RsData<List<ConcertReviewResponse>> {
        val currentUserId = requestContext.actor?.id
        val data = concertReviewService.getList(concertId, currentUserId)
        return RsData("200-1", "리뷰 목록 조회 성공", data)
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "리뷰 상세 조회", description = "콘서트 리뷰 상세 조회 API")
    fun getReview(
        @PathVariable concertId: Long,
        @PathVariable reviewId: Long
    ): RsData<ConcertReviewResponse> {
        val currentUserId = requestContext.actor?.id
        val data = concertReviewService.getDetail(reviewId, currentUserId)
        return RsData("200-1", "리뷰 상세 조회 성공", data)
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "리뷰 수정", description = "콘서트 리뷰 수정 API")
    fun updateReview(
        @PathVariable concertId: Long,
        @PathVariable reviewId: Long,
        @RequestBody @Valid request: ConcertReviewUpdateRequest
    ): RsData<ConcertReviewResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = concertReviewService.update(reviewId, actor.id, request)
        return RsData("200-1", "리뷰가 수정되었습니다.", data)
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "리뷰 삭제", description = "콘서트 리뷰 삭제 API")
    fun deleteReview(
        @PathVariable concertId: Long,
        @PathVariable reviewId: Long
    ): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        concertReviewService.delete(reviewId, actor.id)
        return RsData("200-1", "리뷰가 삭제되었습니다.")
    }
}
