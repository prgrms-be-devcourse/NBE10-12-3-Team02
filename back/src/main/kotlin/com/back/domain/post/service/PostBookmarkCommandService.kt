package com.back.domain.post.service

import com.back.domain.post.entity.PostBookmark
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.post.repository.PostBookmarkRepository
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostBookmarkCommandService(
    private val concertPostRepository: ConcertPostRepository,
    private val postBookmarkRepository: PostBookmarkRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createIfAbsent(postId: Long, userId: Long) {
        val post = concertPostRepository.findById(postId).orElseThrow {
            ServiceException(ErrorCode.POST_NOT_FOUND)
        }
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        if (postBookmarkRepository.existsByPostPostIdAndUserUserId(postId, userId)) {
            return
        }

        postBookmarkRepository.saveAndFlush(PostBookmark.create(post, user))
    }

    @Transactional
    fun delete(postId: Long, userId: Long) {
        if (!concertPostRepository.existsById(postId)) {
            throw ServiceException(ErrorCode.POST_NOT_FOUND)
        }
        postBookmarkRepository.deleteByPostPostIdAndUserUserId(postId, userId)
    }
}
