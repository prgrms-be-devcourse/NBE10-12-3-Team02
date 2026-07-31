package com.back.domain.review.repository

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.review.entity.ConcertReview
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
class ConcertReviewRepositoryTest @Autowired constructor(
    private val concertReviewRepository: ConcertReviewRepository,
    private val concertRepository: ConcertRepository,
    private val userRepository: UserRepository,
) {
    @Test
    @DisplayName("동일 회원은 하나의 콘서트에 리뷰를 중복 저장할 수 없다")
    fun t1() {
        val user = userRepository.saveAndFlush(
            User.create(
                loginId = "review-user",
                email = "review-user@example.com",
                password = "encoded-password",
                name = "리뷰어",
                loginType = LoginType.NORMAL,
            ),
        )
        val concert = concertRepository.saveAndFlush(
            Concert.create(
                concertName = "중복 리뷰 테스트 콘서트",
                description = "테스트 공연",
                startDate = LocalDateTime.now().minusDays(2),
                endDate = LocalDateTime.now().minusDays(1),
                urlPoster = null,
            ),
        )
        concertReviewRepository.saveAndFlush(
            ConcertReview.create(concert, user, "첫 번째 리뷰", "첫 번째 리뷰 내용"),
        )

        assertThatThrownBy {
            concertReviewRepository.saveAndFlush(
                ConcertReview.create(concert, user, "두 번째 리뷰", "두 번째 리뷰 내용"),
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
