package com.back.domain.post.repository

import com.back.domain.concert.entity.Concert
import com.back.domain.post.entity.ConcertPost
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
          )
    """)
    fun findEligibleConcertsForUser(
        @Param("userId") userId: Long,
        @Param("sixMonthsAgo") sixMonthsAgo: LocalDateTime,
        @Param("now") now: LocalDateTime
    ): List<Concert>

    fun findByPostIdAndConcertConcertId(postId: Long, concertId: Long): ConcertPost?

    fun existsByConcert_ConcertIdAndUser_UserId(concertId: Long, userId: Long): Boolean
}
