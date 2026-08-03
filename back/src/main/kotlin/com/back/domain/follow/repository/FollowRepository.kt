package com.back.domain.follow.repository

import com.back.domain.follow.entity.Follow
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FollowRepository : JpaRepository<Follow, Long> {
    fun existsByFollower_UserIdAndFollowee_UserId(followerId: Long, followeeId: Long): Boolean

    fun findByFollower_UserIdAndFollowee_UserId(followerId: Long, followeeId: Long): Follow?

    @Query(
        value = """
            SELECT f
            FROM Follow f
            JOIN FETCH f.follower
            JOIN FETCH f.followee
            WHERE f.follower.userId = :followerId
            ORDER BY f.createDate DESC
        """,
        countQuery = """
            SELECT COUNT(f)
            FROM Follow f
            WHERE f.follower.userId = :followerId
        """,
    )
    fun findAllByFollower_UserId(
        @Param("followerId") followerId: Long,
        pageable: Pageable,
    ): Page<Follow>

    @Query(
        value = """
            SELECT f
            FROM Follow f
            JOIN FETCH f.follower
            JOIN FETCH f.followee
            WHERE f.followee.userId = :followeeId
            ORDER BY f.createDate DESC
        """,
        countQuery = """
            SELECT COUNT(f)
            FROM Follow f
            WHERE f.followee.userId = :followeeId
        """,
    )
    fun findAllByFollowee_UserId(
        @Param("followeeId") followeeId: Long,
        pageable: Pageable,
    ): Page<Follow>
}
