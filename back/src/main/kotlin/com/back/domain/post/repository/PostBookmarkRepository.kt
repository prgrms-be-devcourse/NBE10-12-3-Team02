package com.back.domain.post.repository

import com.back.domain.post.entity.PostBookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostBookmarkRepository : JpaRepository<PostBookmark, Long> {
    fun existsByPostPostIdAndUserUserId(postId: Long, userId: Long): Boolean

    fun deleteByPostPostIdAndUserUserId(postId: Long, userId: Long): Long

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PostBookmark b WHERE b.post.postId = :postId")
    fun deleteAllByPostPostId(@Param("postId") postId: Long): Int

    @Query(
        """
        SELECT b.post.postId
        FROM PostBookmark b
        WHERE b.user.userId = :userId
          AND b.post.postId IN :postIds
        """
    )
    fun findBookmarkedPostIds(
        @Param("postIds") postIds: Collection<Long>,
        @Param("userId") userId: Long,
    ): List<Long>

    @Query(
        value = """
            SELECT b
            FROM PostBookmark b
            JOIN FETCH b.post r
            JOIN FETCH r.user
            JOIN FETCH r.concert
            WHERE b.user.userId = :userId
            ORDER BY b.createDate DESC
        """,
        countQuery = """
            SELECT COUNT(b)
            FROM PostBookmark b
            WHERE b.user.userId = :userId
        """,
    )
    fun findPageByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<PostBookmark>
}
