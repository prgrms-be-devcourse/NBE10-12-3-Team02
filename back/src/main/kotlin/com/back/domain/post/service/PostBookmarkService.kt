package com.back.domain.post.service

import com.back.domain.post.dto.PostBookmarkResponse
import com.back.domain.post.dto.PostBookmarkStatusResponse
import com.back.domain.post.repository.PostBookmarkRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostBookmarkService(
    private val postBookmarkCommandService: PostBookmarkCommandService,
    private val postBookmarkRepository: PostBookmarkRepository,
) {
    fun bookmark(postId: Long, userId: Long): PostBookmarkStatusResponse {
        try {
            postBookmarkCommandService.createIfAbsent(postId, userId)
        } catch (_: DataIntegrityViolationException) {
            // 동시 요청 중 다른 트랜잭션이 먼저 저장한 경우 멱등 성공으로 처리한다.
        }
        return PostBookmarkStatusResponse(isBookmarked = true)
    }

    fun unbookmark(postId: Long, userId: Long): PostBookmarkStatusResponse {
        postBookmarkCommandService.delete(postId, userId)
        return PostBookmarkStatusResponse(isBookmarked = false)
    }

    @Transactional(readOnly = true)
    fun getMyBookmarks(userId: Long, pageable: Pageable): Page<PostBookmarkResponse> =
        postBookmarkRepository.findPageByUserId(userId, pageable)
            .map(PostBookmarkResponse::from)
}
