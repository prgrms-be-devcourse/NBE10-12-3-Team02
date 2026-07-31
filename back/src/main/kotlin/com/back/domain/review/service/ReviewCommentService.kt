package com.back.domain.review.service

import com.back.domain.review.dto.ReviewCommentCreateRequest
import com.back.domain.review.dto.ReviewCommentResponse
import com.back.domain.review.entity.ReviewComment
import com.back.domain.review.repository.ConcertReviewRepository
import com.back.domain.review.repository.ReviewCommentRepository
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReviewCommentService(
    private val reviewCommentRepository: ReviewCommentRepository,
    private val concertReviewRepository: ConcertReviewRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun create(reviewId: Long, userId: Long, request: ReviewCommentCreateRequest): ReviewCommentResponse {
        val review = concertReviewRepository.findById(reviewId).orElseThrow {
            ServiceException(ErrorCode.REVIEW_NOT_FOUND)
        }
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)
        val saved = reviewCommentRepository.save(ReviewComment.create(review, user, request.content))
        return ReviewCommentResponse.of(saved, userId)
    }

    fun getList(reviewId: Long, currentUserId: Long?): List<ReviewCommentResponse> {
        if (!concertReviewRepository.existsById(reviewId)) {
            throw ServiceException(ErrorCode.REVIEW_NOT_FOUND)
        }
        return reviewCommentRepository.findAllByReviewId(reviewId)
            .map { ReviewCommentResponse.of(it, currentUserId) }
    }

    @Transactional
    fun delete(reviewId: Long, commentId: Long, userId: Long) {
        val comment = reviewCommentRepository
            .findByCommentIdAndReviewReviewId(commentId, reviewId)
            ?: throw ServiceException(ErrorCode.COMMENT_NOT_FOUND)
        if (comment.user.userId != userId) {
            throw ServiceException(ErrorCode.COMMENT_FORBIDDEN)
        }
        reviewCommentRepository.delete(comment)
    }
}
