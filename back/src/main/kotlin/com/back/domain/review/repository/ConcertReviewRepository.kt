package com.back.domain.review.repository

import com.back.domain.review.entity.ConcertReview
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ConcertReviewRepository : JpaRepository<ConcertReview, Long> {

    @Query("""
        SELECT r FROM ConcertReview r
        JOIN FETCH r.user
        WHERE r.concert.concertId = :concertId
        ORDER BY r.createDate DESC
    """)
    fun findAllByConcertIdWithUser(@Param("concertId") concertId: Long): List<ConcertReview>

    fun existsByConcert_ConcertIdAndUser_UserId(concertId: Long, userId: Long): Boolean
}
