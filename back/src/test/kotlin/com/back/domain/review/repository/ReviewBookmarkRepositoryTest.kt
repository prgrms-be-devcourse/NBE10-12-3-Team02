package com.back.domain.review.repository

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.review.entity.ConcertReview
import com.back.domain.review.entity.ReviewBookmark
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.jpa.converter.EncryptedStringConverter
import com.back.global.util.AesEncryptionUtil
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
class ReviewBookmarkRepositoryTest @Autowired constructor(
    private val reviewBookmarkRepository: ReviewBookmarkRepository,
    private val concertReviewRepository: ConcertReviewRepository,
    private val concertRepository: ConcertRepository,
    private val userRepository: UserRepository,
) {
    @Test
    @DisplayName("동일 사용자는 하나의 리뷰를 중복 북마크할 수 없다")
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
        val review = concertReviewRepository.saveAndFlush(
            ConcertReview.create(concert, user, "리뷰", "리뷰 내용")
        )
        reviewBookmarkRepository.saveAndFlush(ReviewBookmark.create(review, user))

        assertThatThrownBy {
            reviewBookmarkRepository.saveAndFlush(ReviewBookmark.create(review, user))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
