package com.back.domain.review.controller

import com.back.domain.review.dto.ConcertReviewResponse
import com.back.domain.review.dto.EligibleConcertResponse
import com.back.domain.review.dto.ReviewLikeStatusResponse
import com.back.domain.review.dto.ReviewBookmarkStatusResponse
import com.back.domain.review.service.ConcertReviewService
import com.back.domain.review.service.ReviewBookmarkService
import com.back.domain.review.service.ReviewLikeService
import com.back.global.annotation.ApiV1
import com.back.global.requestcontext.RequestContext
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ApiV1
@RestController
@RequestMapping("/reviews")
@Tag(name = "Review", description = "Review Board API")
class ReviewController(
    private val concertReviewService: ConcertReviewService,
    private val reviewLikeService: ReviewLikeService,
    private val reviewBookmarkService: ReviewBookmarkService,
    private val requestContext: RequestContext
) {

    @GetMapping
    @Operation(summary = "전체 리뷰 피드 조회", description = "모든 콘서트 리뷰를 최신순으로 전체 조회")
    fun getAllReviews(): RsData<List<ConcertReviewResponse>> {
        val currentUserId = requestContext.actor?.id
        val data = concertReviewService.getAllReviews(currentUserId)
        return RsData("200-1", "리뷰 목록 조회 성공", data)
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "리뷰 단건 조회", description = "reviewId로 리뷰 상세 조회 (비인증 허용)")
    fun getReview(@PathVariable reviewId: Long): RsData<ConcertReviewResponse> {
        val currentUserId = requestContext.actor?.id
        val data = concertReviewService.getDetail(reviewId, currentUserId)
        return RsData("200-1", "리뷰 조회 성공", data)
    }

    @PutMapping("/{reviewId}/likes")
    @Operation(summary = "리뷰 좋아요 등록", description = "리뷰에 좋아요를 등록합니다.")
    fun likeReview(@PathVariable reviewId: Long): RsData<ReviewLikeStatusResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        return RsData(
            "200-1",
            "리뷰 좋아요가 등록되었습니다.",
            reviewLikeService.like(reviewId, actor.id),
        )
    }

    @DeleteMapping("/{reviewId}/likes")
    @Operation(summary = "리뷰 좋아요 취소", description = "리뷰 좋아요를 취소합니다.")
    fun unlikeReview(@PathVariable reviewId: Long): RsData<ReviewLikeStatusResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        return RsData(
            "200-1",
            "리뷰 좋아요가 취소되었습니다.",
            reviewLikeService.unlike(reviewId, actor.id),
        )
    }

    @PutMapping("/{reviewId}/bookmarks")
    @Operation(summary = "리뷰 북마크 등록", description = "리뷰를 내 북마크에 등록합니다.")
    fun bookmarkReview(@PathVariable reviewId: Long): RsData<ReviewBookmarkStatusResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        return RsData(
            "200-1",
            "리뷰가 북마크에 등록되었습니다.",
            reviewBookmarkService.bookmark(reviewId, actor.id),
        )
    }

    @DeleteMapping("/{reviewId}/bookmarks")
    @Operation(summary = "리뷰 북마크 취소", description = "리뷰를 내 북마크에서 제거합니다.")
    fun unbookmarkReview(@PathVariable reviewId: Long): RsData<ReviewBookmarkStatusResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        return RsData(
            "200-1",
            "리뷰 북마크가 취소되었습니다.",
            reviewBookmarkService.unbookmark(reviewId, actor.id),
        )
    }

    @GetMapping("/eligible-concerts")
    @Operation(summary = "리뷰 작성 가능 콘서트 목록", description = "현재 로그인 유저가 리뷰를 쓸 수 있는 콘서트 목록")
    fun getEligibleConcerts(): RsData<List<EligibleConcertResponse>> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = concertReviewService.getEligibleConcerts(actor.id)
        return RsData("200-1", "리뷰 작성 가능 콘서트 목록 조회 성공", data)
    }
}
