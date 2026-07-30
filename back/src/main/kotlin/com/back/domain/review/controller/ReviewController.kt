package com.back.domain.review.controller

import com.back.domain.review.dto.ConcertReviewResponse
import com.back.domain.review.dto.EligibleConcertResponse
import com.back.domain.review.service.ConcertReviewService
import com.back.global.annotation.ApiV1
import com.back.global.requestcontext.RequestContext
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ApiV1
@RestController
@RequestMapping("/reviews")
@Tag(name = "Review", description = "Review Board API")
class ReviewController(
    private val concertReviewService: ConcertReviewService,
    private val requestContext: RequestContext
) {

    @GetMapping
    @Operation(summary = "전체 리뷰 피드 조회", description = "모든 콘서트 리뷰를 최신순으로 전체 조회")
    fun getAllReviews(): RsData<List<ConcertReviewResponse>> {
        val currentUserId = requestContext.actor?.id
        val data = concertReviewService.getAllReviews(currentUserId)
        return RsData("200-1", "리뷰 목록 조회 성공", data)
    }

    @GetMapping("/eligible-concerts")
    @Operation(summary = "리뷰 작성 가능 콘서트 목록", description = "현재 로그인 유저가 리뷰를 쓸 수 있는 콘서트 목록")
    fun getEligibleConcerts(): RsData<List<EligibleConcertResponse>> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = concertReviewService.getEligibleConcerts(actor.id)
        return RsData("200-1", "리뷰 작성 가능 콘서트 목록 조회 성공", data)
    }
}
