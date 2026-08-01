package com.back.domain.follow.service

import com.back.domain.follow.dto.FollowStatusResponse
import com.back.domain.follow.dto.FollowUserResponse
import com.back.domain.follow.entity.Follow
import com.back.domain.follow.repository.FollowRepository
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun follow(followerId: Long, followeeId: Long) {
        if (followerId == followeeId) {
            throw ServiceException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED)
        }

        val follower = userRepository.findByUserIdAndDeletedAtIsNull(followerId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)
        val followee = userRepository.findByUserIdAndDeletedAtIsNull(followeeId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        if (followRepository.existsByFollower_UserIdAndFollowee_UserId(followerId, followeeId)) {
            throw ServiceException(ErrorCode.FOLLOW_ALREADY_EXISTS)
        }

        try {
            followRepository.saveAndFlush(Follow.create(follower, followee))
        } catch (e: DataIntegrityViolationException) {
            throw ServiceException(ErrorCode.FOLLOW_ALREADY_EXISTS)
        }
    }

    @Transactional
    fun unfollow(followerId: Long, followeeId: Long) {
        val follow = followRepository.findByFollower_UserIdAndFollowee_UserId(followerId, followeeId)
            ?: throw ServiceException(ErrorCode.FOLLOW_NOT_FOUND)
        followRepository.delete(follow)
    }

    fun isFollowing(followerId: Long, followeeId: Long): FollowStatusResponse =
        FollowStatusResponse(
            isFollowing = followRepository.existsByFollower_UserIdAndFollowee_UserId(followerId, followeeId)
        )

    fun getFollowings(followerId: Long, pageable: Pageable): Page<FollowUserResponse> =
        followRepository.findAllByFollower_UserId(followerId, pageable)
            .map { FollowUserResponse.ofFollowee(it) }

    fun getFollowers(followeeId: Long, pageable: Pageable): Page<FollowUserResponse> =
        followRepository.findAllByFollowee_UserId(followeeId, pageable)
            .map { FollowUserResponse.ofFollower(it) }
}
