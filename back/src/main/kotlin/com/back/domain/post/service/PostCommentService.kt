package com.back.domain.post.service

import com.back.domain.post.dto.PostCommentCreateRequest
import com.back.domain.post.dto.PostCommentResponse
import com.back.domain.post.dto.PostCommentUpdateRequest
import com.back.domain.post.entity.PostComment
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.post.repository.PostCommentRepository
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostCommentService(
    private val postCommentRepository: PostCommentRepository,
    private val concertPostRepository: ConcertPostRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun create(postId: Long, userId: Long, request: PostCommentCreateRequest): PostCommentResponse {
        val post = concertPostRepository.findById(postId).orElseThrow {
            ServiceException(ErrorCode.POST_NOT_FOUND)
        }
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)
        val saved = postCommentRepository.save(PostComment.create(post, user, request.content))
        return PostCommentResponse.of(saved, userId)
    }

    fun getList(postId: Long, currentUserId: Long?): List<PostCommentResponse> {
        if (!concertPostRepository.existsById(postId)) {
            throw ServiceException(ErrorCode.POST_NOT_FOUND)
        }
        return postCommentRepository.findAllByPostId(postId)
            .map { PostCommentResponse.of(it, currentUserId) }
    }

    @Transactional
    fun update(postId: Long, commentId: Long, userId: Long, request: PostCommentUpdateRequest): PostCommentResponse {
        val comment = postCommentRepository
            .findByCommentIdAndPostPostId(commentId, postId)
            ?: throw ServiceException(ErrorCode.COMMENT_NOT_FOUND)
        if (comment.user.userId != userId) {
            throw ServiceException(ErrorCode.COMMENT_FORBIDDEN)
        }
        comment.update(request.content)
        return PostCommentResponse.of(comment, userId)
    }

    @Transactional
    fun delete(postId: Long, commentId: Long, userId: Long) {
        val comment = postCommentRepository
            .findByCommentIdAndPostPostId(commentId, postId)
            ?: throw ServiceException(ErrorCode.COMMENT_NOT_FOUND)
        if (comment.user.userId != userId) {
            throw ServiceException(ErrorCode.COMMENT_FORBIDDEN)
        }
        postCommentRepository.delete(comment)
    }
}
