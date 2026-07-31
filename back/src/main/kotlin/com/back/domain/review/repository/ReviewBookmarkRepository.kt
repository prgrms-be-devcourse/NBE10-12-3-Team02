package com.back.domain.review.repository

import com.back.domain.review.entity.ReviewBookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReviewBookmarkRepository : JpaRepository<ReviewBookmark, Long> {
    fun existsByReviewReviewIdAndUserUserId(reviewId: Long, userId: Long): Boolean

    fun deleteByReviewReviewIdAndUserUserId(reviewId: Long, userId: Long): Long

    fun deleteAllByReviewReviewId(reviewId: Long): Long

    @Query(
        """
        SELECT b.review.reviewId
        FROM ReviewBookmark b
        WHERE b.user.userId = :userId
          AND b.review.reviewId IN :reviewIds
        """
    )
    fun findBookmarkedReviewIds(
        @Param("reviewIds") reviewIds: Collection<Long>,
        @Param("userId") userId: Long,
    ): List<Long>

    @Query(
        value = """
            SELECT b
            FROM ReviewBookmark b
            JOIN FETCH b.review r
            JOIN FETCH r.user
            JOIN FETCH r.concert
            WHERE b.user.userId = :userId
            ORDER BY b.createDate DESC
        """,
        countQuery = """
            SELECT COUNT(b)
            FROM ReviewBookmark b
            WHERE b.user.userId = :userId
        """,
    )
    fun findPageByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<ReviewBookmark>
}
