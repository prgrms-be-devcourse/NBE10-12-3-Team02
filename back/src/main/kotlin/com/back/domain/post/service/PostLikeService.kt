package com.back.domain.post.service

import com.back.domain.post.dto.PostLikeStatusResponse
import com.back.domain.post.repository.PostLikeRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class PostLikeService(
    private val postLikeCommandService: PostLikeCommandService,
    private val postLikeRepository: PostLikeRepository,
) {
    fun like(postId: Long, userId: Long): PostLikeStatusResponse {
        try {
            postLikeCommandService.createIfAbsent(postId, userId)
        } catch (e: DataIntegrityViolationException) {
            // 실제 좋아요가 저장된 경우에만 동시 중복 요청으로 판단한다.
            if (!postLikeRepository.existsByPostPostIdAndUserUserId(postId, userId)) {
                throw e
            }
        }
        return getStatus(postId, userId)
    }

    fun unlike(postId: Long, userId: Long): PostLikeStatusResponse {
        postLikeCommandService.delete(postId, userId)
        return PostLikeStatusResponse(
            liked = false,
            likeCount = postLikeRepository.countByPostPostId(postId),
        )
    }

    private fun getStatus(postId: Long, userId: Long): PostLikeStatusResponse =
        PostLikeStatusResponse(
            liked = postLikeRepository.existsByPostPostIdAndUserUserId(postId, userId),
            likeCount = postLikeRepository.countByPostPostId(postId),
        )
}
