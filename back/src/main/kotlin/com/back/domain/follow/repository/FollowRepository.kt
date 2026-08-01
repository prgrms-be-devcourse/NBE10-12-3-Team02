package com.back.domain.follow.repository

import com.back.domain.follow.entity.Follow
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface FollowRepository : JpaRepository<Follow, Long> {
    fun existsByFollower_UserIdAndFollowee_UserId(followerId: Long, followeeId: Long): Boolean

    fun findByFollower_UserIdAndFollowee_UserId(followerId: Long, followeeId: Long): Follow?

    fun findAllByFollower_UserId(followerId: Long, pageable: Pageable): Page<Follow>

    fun findAllByFollowee_UserId(followeeId: Long, pageable: Pageable): Page<Follow>
}
