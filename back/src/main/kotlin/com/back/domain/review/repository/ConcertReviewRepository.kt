package com.back.domain.review.repository

import com.back.domain.concert.entity.Concert
import com.back.domain.review.entity.ConcertReview
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ConcertReviewRepository : JpaRepository<ConcertReview, Long> {

    @Query("""
        SELECT r FROM ConcertReview r
        JOIN FETCH r.user
        WHERE r.concert.concertId = :concertId
        ORDER BY r.createDate DESC
    """)
    fun findAllByConcertIdWithUser(@Param("concertId") concertId: Long): List<ConcertReview>

    @Query("""
        SELECT r FROM ConcertReview r
        JOIN FETCH r.user
        JOIN FETCH r.concert
        ORDER BY r.createDate DESC
    """)
    fun findAllWithConcertAndUser(): List<ConcertReview>

    @Query("""
        SELECT DISTINCT t.schedule.concert
        FROM Ticket t
        WHERE t.user.userId = :userId
          AND t.isValid = true
          AND t.schedule.scheduleDate BETWEEN :sixMonthsAgo AND :now
          AND NOT EXISTS (
            SELECT r FROM ConcertReview r
            WHERE r.concert = t.schedule.concert
              AND r.user.userId = :userId
          )
    """)
    fun findEligibleConcertsForUser(
        @Param("userId") userId: Long,
        @Param("sixMonthsAgo") sixMonthsAgo: LocalDateTime,
        @Param("now") now: LocalDateTime
    ): List<Concert>

    fun findByReviewIdAndConcertConcertId(reviewId: Long, concertId: Long): ConcertReview?

    fun existsByConcert_ConcertIdAndUser_UserId(concertId: Long, userId: Long): Boolean
}
