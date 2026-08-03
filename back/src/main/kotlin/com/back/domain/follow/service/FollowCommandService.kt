package com.back.domain.follow.service

import com.back.domain.follow.entity.Follow
import com.back.domain.follow.repository.FollowRepository
import com.back.domain.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FollowCommandService(
    private val followRepository: FollowRepository,
) {
    @Transactional
    fun save(follower: User, followee: User) {
        followRepository.saveAndFlush(Follow.create(follower, followee))
    }
}
