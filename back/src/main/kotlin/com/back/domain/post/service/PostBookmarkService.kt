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
        } catch (e: DataIntegrityViolationException) {
            // 실제 북마크가 저장된 경우에만 동시 중복 요청으로 판단한다.
            if (!postBookmarkRepository.existsByPostPostIdAndUserUserId(postId, userId)) {
                throw e
            }
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
