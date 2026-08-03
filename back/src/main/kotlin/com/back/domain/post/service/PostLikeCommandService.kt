package com.back.domain.post.service

import com.back.domain.notification.event.PostLikedEvent
import com.back.domain.post.entity.PostLike
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.post.repository.PostLikeRepository
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostLikeCommandService(
    private val concertPostRepository: ConcertPostRepository,
    private val postLikeRepository: PostLikeRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun createIfAbsent(postId: Long, userId: Long) {
        val post = concertPostRepository.findById(postId).orElseThrow {
            ServiceException(ErrorCode.POST_NOT_FOUND)
        }
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        if (postLikeRepository.existsByPostPostIdAndUserUserId(postId, userId)) {
            return
        }

        postLikeRepository.saveAndFlush(PostLike.create(post, user))

        val postOwnerId = post.user.userIdOrThrow
        if (postOwnerId != userId) {
            eventPublisher.publishEvent(PostLikedEvent(postId, postOwnerId, userId))
        }
    }

    @Transactional
    fun delete(postId: Long, userId: Long) {
        if (!concertPostRepository.existsById(postId)) {
            throw ServiceException(ErrorCode.POST_NOT_FOUND)
        }
        postLikeRepository.deleteByPostPostIdAndUserUserId(postId, userId)
    }
}
