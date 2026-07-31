package com.back.domain.review.service

import com.back.domain.review.entity.ReviewBookmark
import com.back.domain.review.repository.ConcertReviewRepository
import com.back.domain.review.repository.ReviewBookmarkRepository
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewBookmarkCommandService(
    private val concertReviewRepository: ConcertReviewRepository,
    private val reviewBookmarkRepository: ReviewBookmarkRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createIfAbsent(reviewId: Long, userId: Long) {
        val review = concertReviewRepository.findById(reviewId).orElseThrow {
            ServiceException(ErrorCode.REVIEW_NOT_FOUND)
        }
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        if (reviewBookmarkRepository.existsByReviewReviewIdAndUserUserId(reviewId, userId)) {
            return
        }

        reviewBookmarkRepository.saveAndFlush(ReviewBookmark.create(review, user))
    }

    @Transactional
    fun delete(reviewId: Long, userId: Long) {
        if (!concertReviewRepository.existsById(reviewId)) {
            throw ServiceException(ErrorCode.REVIEW_NOT_FOUND)
        }
        reviewBookmarkRepository.deleteByReviewReviewIdAndUserUserId(reviewId, userId)
    }
}
