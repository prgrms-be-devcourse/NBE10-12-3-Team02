package com.back.domain.post.service

import com.back.domain.post.repository.PostBookmarkRepository
import com.back.domain.post.repository.PostLikeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException

class PostInteractionServiceTest {
    @Test
    @DisplayName("좋아요 저장 무결성 오류 후 좋아요가 존재하면 동시 중복 요청으로 처리한다")
    fun t1() {
        val commandService = mock(PostLikeCommandService::class.java)
        val repository = mock(PostLikeRepository::class.java)
        val service = PostLikeService(commandService, repository)
        val exception = DataIntegrityViolationException("duplicate like")

        doThrow(exception).`when`(commandService).createIfAbsent(1L, 2L)
        `when`(repository.existsByPostPostIdAndUserUserId(1L, 2L)).thenReturn(true)
        `when`(repository.countByPostPostId(1L)).thenReturn(1L)

        val result = service.like(1L, 2L)

        assertThat(result.isLiked).isTrue
        assertThat(result.likeCount).isEqualTo(1L)
    }

    @Test
    @DisplayName("좋아요 저장 무결성 오류 후 좋아요가 없으면 원래 예외를 전달한다")
    fun t2() {
        val commandService = mock(PostLikeCommandService::class.java)
        val repository = mock(PostLikeRepository::class.java)
        val service = PostLikeService(commandService, repository)
        val exception = DataIntegrityViolationException("foreign key violation")

        doThrow(exception).`when`(commandService).createIfAbsent(1L, 2L)
        `when`(repository.existsByPostPostIdAndUserUserId(1L, 2L)).thenReturn(false)

        assertThatThrownBy { service.like(1L, 2L) }
            .isSameAs(exception)
    }

    @Test
    @DisplayName("북마크 저장 무결성 오류 후 북마크가 존재하면 동시 중복 요청으로 처리한다")
    fun t3() {
        val commandService = mock(PostBookmarkCommandService::class.java)
        val repository = mock(PostBookmarkRepository::class.java)
        val service = PostBookmarkService(commandService, repository)
        val exception = DataIntegrityViolationException("duplicate bookmark")

        doThrow(exception).`when`(commandService).createIfAbsent(1L, 2L)
        `when`(repository.existsByPostPostIdAndUserUserId(1L, 2L)).thenReturn(true)

        val result = service.bookmark(1L, 2L)

        assertThat(result.isBookmarked).isTrue
    }

    @Test
    @DisplayName("북마크 저장 무결성 오류 후 북마크가 없으면 원래 예외를 전달한다")
    fun t4() {
        val commandService = mock(PostBookmarkCommandService::class.java)
        val repository = mock(PostBookmarkRepository::class.java)
        val service = PostBookmarkService(commandService, repository)
        val exception = DataIntegrityViolationException("foreign key violation")

        doThrow(exception).`when`(commandService).createIfAbsent(1L, 2L)
        `when`(repository.existsByPostPostIdAndUserUserId(1L, 2L)).thenReturn(false)

        assertThatThrownBy { service.bookmark(1L, 2L) }
            .isSameAs(exception)
    }
}
