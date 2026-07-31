package com.back.domain.review.service

import com.back.domain.review.dto.ReviewBookmarkResponse
import com.back.domain.review.dto.ReviewBookmarkStatusResponse
import com.back.domain.review.repository.ReviewBookmarkRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewBookmarkService(
    private val reviewBookmarkCommandService: ReviewBookmarkCommandService,
    private val reviewBookmarkRepository: ReviewBookmarkRepository,
) {
    fun bookmark(reviewId: Long, userId: Long): ReviewBookmarkStatusResponse {
        try {
            reviewBookmarkCommandService.createIfAbsent(reviewId, userId)
        } catch (_: DataIntegrityViolationException) {
            // 동시 요청 중 다른 트랜잭션이 먼저 저장한 경우 멱등 성공으로 처리한다.
        }
        return ReviewBookmarkStatusResponse(isBookmarked = true)
    }

    fun unbookmark(reviewId: Long, userId: Long): ReviewBookmarkStatusResponse {
        reviewBookmarkCommandService.delete(reviewId, userId)
        return ReviewBookmarkStatusResponse(isBookmarked = false)
    }

    @Transactional(readOnly = true)
    fun getMyBookmarks(userId: Long, pageable: Pageable): Page<ReviewBookmarkResponse> =
        reviewBookmarkRepository.findPageByUserId(userId, pageable)
            .map(ReviewBookmarkResponse::from)
}
