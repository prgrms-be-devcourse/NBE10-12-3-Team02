package com.back.domain.post.service

import com.back.domain.post.dto.PostLikeResponse
import com.back.domain.post.dto.PostLikeStatusResponse
import com.back.domain.post.repository.PostLikeRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
        return PostLikeStatusResponse(
            isLiked = true,
            likeCount = postLikeRepository.countByPostPostId(postId),
        )
    }

    fun unlike(postId: Long, userId: Long): PostLikeStatusResponse {
        postLikeCommandService.delete(postId, userId)
        return PostLikeStatusResponse(
            isLiked = false,
            likeCount = postLikeRepository.countByPostPostId(postId),
        )
    }

    @Transactional(readOnly = true)
    fun getMyLikes(userId: Long, pageable: Pageable): Page<PostLikeResponse> =
        postLikeRepository.findPageByUserId(userId, pageable)
            .map(PostLikeResponse::from)

}
