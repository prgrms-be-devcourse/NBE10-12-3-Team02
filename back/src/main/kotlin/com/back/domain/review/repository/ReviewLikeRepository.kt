package com.back.domain.review.repository

import com.back.domain.review.entity.ReviewLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReviewLikeRepository : JpaRepository<ReviewLike, Long> {
    fun existsByReviewReviewIdAndUserUserId(reviewId: Long, userId: Long): Boolean

    fun countByReviewReviewId(reviewId: Long): Long

    fun deleteByReviewReviewIdAndUserUserId(reviewId: Long, userId: Long): Long

    fun deleteAllByReviewReviewId(reviewId: Long): Long

    @Query(
        """
        SELECT l.review.reviewId AS reviewId, COUNT(l) AS likeCount
        FROM ReviewLike l
        WHERE l.review.reviewId IN :reviewIds
        GROUP BY l.review.reviewId
        """
    )
    fun countByReviewIds(
        @Param("reviewIds") reviewIds: Collection<Long>,
    ): List<ReviewLikeCountProjection>

    @Query(
        """
        SELECT l.review.reviewId
        FROM ReviewLike l
        WHERE l.user.userId = :userId
          AND l.review.reviewId IN :reviewIds
        """
    )
    fun findLikedReviewIds(
        @Param("reviewIds") reviewIds: Collection<Long>,
        @Param("userId") userId: Long,
    ): List<Long>
}
