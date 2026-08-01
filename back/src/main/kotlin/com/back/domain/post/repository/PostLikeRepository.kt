package com.back.domain.post.repository

import com.back.domain.post.entity.PostLike
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostLikeRepository : JpaRepository<PostLike, Long> {
    fun existsByPostPostIdAndUserUserId(postId: Long, userId: Long): Boolean

    fun countByPostPostId(postId: Long): Long

    fun deleteByPostPostIdAndUserUserId(postId: Long, userId: Long): Long

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PostLike l WHERE l.post.postId = :postId")
    fun deleteAllByPostPostId(@Param("postId") postId: Long): Int

    @Query(
        """
        SELECT l.post.postId AS postId, COUNT(l) AS likeCount
        FROM PostLike l
        WHERE l.post.postId IN :postIds
        GROUP BY l.post.postId
        """
    )
    fun countByPostIds(
        @Param("postIds") postIds: Collection<Long>,
    ): List<PostLikeCountProjection>

    @Query(
        """
        SELECT l.post.postId
        FROM PostLike l
        WHERE l.user.userId = :userId
          AND l.post.postId IN :postIds
        """
    )
    fun findLikedPostIds(
        @Param("postIds") postIds: Collection<Long>,
        @Param("userId") userId: Long,
    ): List<Long>

    @Query(
        value = """
            SELECT l
            FROM PostLike l
            JOIN FETCH l.post r
            JOIN FETCH r.user
            JOIN FETCH r.concert
            WHERE l.user.userId = :userId
            ORDER BY l.createDate DESC
        """,
        countQuery = """
            SELECT COUNT(l)
            FROM PostLike l
            WHERE l.user.userId = :userId
        """,
    )
    fun findPageByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<PostLike>
}
