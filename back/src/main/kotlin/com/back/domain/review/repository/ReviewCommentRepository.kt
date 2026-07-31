package com.back.domain.review.repository

import com.back.domain.review.entity.ReviewComment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReviewCommentRepository : JpaRepository<ReviewComment, Long> {

    @Query("""
        SELECT c FROM ReviewComment c
        JOIN FETCH c.user
        WHERE c.review.reviewId = :reviewId
        ORDER BY c.createDate ASC
    """)
    fun findAllByReviewId(@Param("reviewId") reviewId: Long): List<ReviewComment>
}
