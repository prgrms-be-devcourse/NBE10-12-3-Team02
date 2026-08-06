package com.back.domain.post.repository

import com.back.domain.concert.entity.Concert
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.ReviewType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ConcertPostRepository : JpaRepository<ConcertPost, Long> {

    @Query("""
        SELECT r FROM ConcertPost r
        JOIN FETCH r.user
        WHERE r.concert.concertId = :concertId
        ORDER BY r.createDate DESC
    """)
    fun findAllByConcertIdWithUser(@Param("concertId") concertId: Long): List<ConcertPost>

    @Query("""
        SELECT r FROM ConcertPost r
        JOIN FETCH r.user
        JOIN FETCH r.concert
        ORDER BY r.createDate DESC
    """)
    fun findAllWithConcertAndUser(): List<ConcertPost>

    @Query("""
        SELECT DISTINCT t.schedule.concert
        FROM Ticket t
        WHERE t.user.userId = :userId
          AND t.isValid = true
          AND t.schedule.scheduleDate BETWEEN :sixMonthsAgo AND :now
          AND NOT EXISTS (
            SELECT r FROM ConcertPost r
            WHERE r.concert = t.schedule.concert
              AND r.user.userId = :userId
              AND r.reviewType = :reviewType
          )
    """)
    fun findEligibleConcertsForReview(
        @Param("userId") userId: Long,
        @Param("sixMonthsAgo") sixMonthsAgo: LocalDateTime,
        @Param("now") now: LocalDateTime,
        @Param("reviewType") reviewType: ReviewType,
    ): List<Concert>

    @Query("""
        SELECT DISTINCT t.schedule.concert
        FROM Ticket t
        WHERE t.user.userId = :userId
          AND NOT EXISTS (
            SELECT r FROM ConcertPost r
            WHERE r.concert = t.schedule.concert
              AND r.user.userId = :userId
              AND r.reviewType = :reviewType
          )
    """)
    fun findEligibleConcertsForExpectation(
        @Param("userId") userId: Long,
        @Param("reviewType") reviewType: ReviewType,
    ): List<Concert>

    @Query(
        value = """
            SELECT r FROM ConcertPost r
            JOIN FETCH r.user
            JOIN FETCH r.concert
            WHERE r.user.userId = :userId
            ORDER BY r.createDate DESC
        """,
        countQuery = """
            SELECT COUNT(r)
            FROM ConcertPost r
            WHERE r.user.userId = :userId
        """,
    )
    fun findAllByUser_UserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<ConcertPost>

    fun findByPostIdAndConcertConcertId(postId: Long, concertId: Long): ConcertPost?

    fun existsByConcert_ConcertIdAndUser_UserIdAndReviewType(concertId: Long, userId: Long, reviewType: ReviewType): Boolean

    fun countByConcert_ConcertIdAndReviewType(concertId: Long, reviewType: ReviewType): Long

    fun findTop20ByConcert_ConcertIdAndReviewTypeOrderByCreateDateDesc(
        concertId: Long,
        reviewType: ReviewType,
    ): List<ConcertPost>
}
