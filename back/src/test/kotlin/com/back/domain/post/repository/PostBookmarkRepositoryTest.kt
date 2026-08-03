package com.back.domain.post.repository

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.PostBookmark
import com.back.domain.post.entity.ReviewType
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.jpa.converter.EncryptedStringConverter
import com.back.global.util.AesEncryptionUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

@DataJpaTest
@Import(AesEncryptionUtil::class, EncryptedStringConverter::class)
class PostBookmarkRepositoryTest @Autowired constructor(
    private val postBookmarkRepository: PostBookmarkRepository,
    private val concertPostRepository: ConcertPostRepository,
    private val concertRepository: ConcertRepository,
    private val userRepository: UserRepository,
) {
    @Test
    @DisplayName("동일 사용자는 하나의 게시글을 중복 북마크할 수 없다")
    fun t1() {
        val user = userRepository.saveAndFlush(
            User.create(
                "bookmark-repository-user",
                "bookmark-repository-user@example.com",
                "encoded-password",
                "북마크사용자",
                LoginType.NORMAL,
            )
        )
        val concert = concertRepository.saveAndFlush(
            Concert.create(
                "북마크 제약 테스트 콘서트",
                "테스트 공연",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                null,
            )
        )
        val post = concertPostRepository.saveAndFlush(
            ConcertPost.create(concert, user, "게시글", "게시글 내용", rating = 5, reviewType = ReviewType.REVIEW)
        )
        postBookmarkRepository.saveAndFlush(PostBookmark.create(post, user))

        assertThatThrownBy {
            postBookmarkRepository.saveAndFlush(PostBookmark.create(post, user))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    @DisplayName("게시글의 북마크를 단일 벌크 쿼리로 삭제하고 다른 게시글 북마크는 유지한다")
    fun t2() {
        val user1 = userRepository.saveAndFlush(
            User.create("bulk-bookmark-user-1", "bulk-bookmark-1@example.com", "password", "사용자1", LoginType.NORMAL)
        )
        val user2 = userRepository.saveAndFlush(
            User.create("bulk-bookmark-user-2", "bulk-bookmark-2@example.com", "password", "사용자2", LoginType.NORMAL)
        )
        val concert1 = concertRepository.saveAndFlush(
            Concert.create(
                "벌크 북마크 콘서트 1",
                "테스트 공연",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                null,
            )
        )
        val concert2 = concertRepository.saveAndFlush(
            Concert.create(
                "벌크 북마크 콘서트 2",
                "테스트 공연",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                null,
            )
        )
        val targetPost = concertPostRepository.saveAndFlush(ConcertPost.create(concert1, user1, "대상", "내용", rating = 5, reviewType = ReviewType.REVIEW))
        val otherPost = concertPostRepository.saveAndFlush(ConcertPost.create(concert2, user1, "다른 게시글", "내용", rating = 5, reviewType = ReviewType.REVIEW))
        postBookmarkRepository.saveAllAndFlush(
            listOf(
                PostBookmark.create(targetPost, user1),
                PostBookmark.create(targetPost, user2),
                PostBookmark.create(otherPost, user1),
            )
        )

        val deletedCount = postBookmarkRepository.deleteAllByPostPostId(targetPost.postId!!)

        assertThat(deletedCount).isEqualTo(2)
        assertThat(
            postBookmarkRepository.existsByPostPostIdAndUserUserId(targetPost.postId!!, user1.userId!!)
        ).isFalse()
        assertThat(
            postBookmarkRepository.existsByPostPostIdAndUserUserId(otherPost.postId!!, user1.userId!!)
        ).isTrue()
    }
}
