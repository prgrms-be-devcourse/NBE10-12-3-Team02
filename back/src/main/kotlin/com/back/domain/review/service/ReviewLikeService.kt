package com.back.domain.review.service

import com.back.domain.review.dto.ReviewLikeStatusResponse
import com.back.domain.review.repository.ReviewLikeRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class ReviewLikeService(
    private val reviewLikeCommandService: ReviewLikeCommandService,
    private val reviewLikeRepository: ReviewLikeRepository,
) {
    fun like(reviewId: Long, userId: Long): ReviewLikeStatusResponse {
        try {
            reviewLikeCommandService.createIfAbsent(reviewId, userId)
        } catch (_: DataIntegrityViolationException) {
            // 동시 요청 중 다른 트랜잭션이 먼저 저장한 경우 멱등 성공으로 처리한다.
        }
        return getStatus(reviewId, userId)
    }

    fun unlike(reviewId: Long, userId: Long): ReviewLikeStatusResponse {
        reviewLikeCommandService.delete(reviewId, userId)
        return ReviewLikeStatusResponse(
            liked = false,
            likeCount = reviewLikeRepository.countByReviewReviewId(reviewId),
        )
    }

    private fun getStatus(reviewId: Long, userId: Long): ReviewLikeStatusResponse =
        ReviewLikeStatusResponse(
            liked = reviewLikeRepository.existsByReviewReviewIdAndUserUserId(reviewId, userId),
            likeCount = reviewLikeRepository.countByReviewReviewId(reviewId),
        )
}
