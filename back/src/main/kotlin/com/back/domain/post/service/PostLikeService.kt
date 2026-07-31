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
        } catch (_: DataIntegrityViolationException) {
            // 동시 요청 중 다른 트랜잭션이 먼저 저장한 경우 멱등 성공으로 처리한다.
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
