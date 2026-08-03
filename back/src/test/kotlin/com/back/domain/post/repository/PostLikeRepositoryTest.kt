package com.back.domain.post.repository

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.PostLike
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
class PostLikeRepositoryTest @Autowired constructor(
    private val postLikeRepository: PostLikeRepository,
    private val concertPostRepository: ConcertPostRepository,
    private val concertRepository: ConcertRepository,
    private val userRepository: UserRepository,
) {
    @Test
    @DisplayName("동일 사용자는 하나의 게시글에 좋아요를 중복 저장할 수 없다")
    fun t1() {
        val user = userRepository.saveAndFlush(
            User.create(
                "like-user",
                "like-user@example.com",
                "encoded-password",
                "좋아요사용자",
                LoginType.NORMAL,
            )
        )
        val concert = concertRepository.saveAndFlush(
            Concert.create(
                "좋아요 제약 테스트 콘서트",
                "테스트 공연",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                null,
            )
        )
        val post = concertPostRepository.saveAndFlush(
            ConcertPost.create(concert, user, "게시글", "게시글 내용", rating = 5, reviewType = ReviewType.REVIEW)
        )
        postLikeRepository.saveAndFlush(PostLike.create(post, user))

        assertThatThrownBy {
            postLikeRepository.saveAndFlush(PostLike.create(post, user))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    @DisplayName("게시글의 좋아요를 단일 벌크 쿼리로 삭제하고 다른 게시글 좋아요는 유지한다")
    fun t2() {
        val user1 = userRepository.saveAndFlush(
            User.create("bulk-like-user-1", "bulk-like-1@example.com", "password", "사용자1", LoginType.NORMAL)
        )
        val user2 = userRepository.saveAndFlush(
            User.create("bulk-like-user-2", "bulk-like-2@example.com", "password", "사용자2", LoginType.NORMAL)
        )
        val concert1 = concertRepository.saveAndFlush(
            Concert.create(
                "벌크 좋아요 콘서트 1",
                "테스트 공연",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                null,
            )
        )
        val concert2 = concertRepository.saveAndFlush(
            Concert.create(
                "벌크 좋아요 콘서트 2",
                "테스트 공연",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                null,
            )
        )
        val targetPost = concertPostRepository.saveAndFlush(ConcertPost.create(concert1, user1, "대상", "내용", rating = 5, reviewType = ReviewType.REVIEW))
        val otherPost = concertPostRepository.saveAndFlush(ConcertPost.create(concert2, user1, "다른 게시글", "내용", rating = 5, reviewType = ReviewType.REVIEW))
        postLikeRepository.saveAllAndFlush(
            listOf(
                PostLike.create(targetPost, user1),
                PostLike.create(targetPost, user2),
                PostLike.create(otherPost, user1),
            )
        )

        val deletedCount = postLikeRepository.deleteAllByPostPostId(targetPost.postId!!)

        assertThat(deletedCount).isEqualTo(2)
        assertThat(postLikeRepository.countByPostPostId(targetPost.postId!!)).isZero()
        assertThat(postLikeRepository.countByPostPostId(otherPost.postId!!)).isEqualTo(1)
    }
}
