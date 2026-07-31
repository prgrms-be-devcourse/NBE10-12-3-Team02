package com.back.domain.post.repository

import com.back.domain.post.entity.PostComment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostCommentRepository : JpaRepository<PostComment, Long> {

    @Query("""
        SELECT c FROM PostComment c
        JOIN FETCH c.user
        WHERE c.post.postId = :postId
        ORDER BY c.createDate ASC
    """)
    fun findAllByPostId(@Param("postId") postId: Long): List<PostComment>

    fun findByCommentIdAndPostPostId(commentId: Long, postId: Long): PostComment?
}
