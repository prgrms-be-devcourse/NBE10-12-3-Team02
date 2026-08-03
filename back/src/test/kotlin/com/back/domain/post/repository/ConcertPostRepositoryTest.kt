package com.back.domain.post.repository

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.ReviewType
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
class ConcertPostRepositoryTest @Autowired constructor(
    private val concertPostRepository: ConcertPostRepository,
    private val concertRepository: ConcertRepository,
    private val userRepository: UserRepository,
) {
    @Test
    @DisplayName("동일 회원은 하나의 콘서트에 게시글을 중복 저장할 수 없다")
    fun t1() {
        val user = userRepository.saveAndFlush(
            User.create(
                loginId = "post-user",
                email = "post-user@example.com",
                password = "encoded-password",
                name = "게시글어",
                loginType = LoginType.NORMAL,
            ),
        )
        val concert = concertRepository.saveAndFlush(
            Concert.create(
                concertName = "중복 게시글 테스트 콘서트",
                description = "테스트 공연",
                startDate = LocalDateTime.now().minusDays(2),
                endDate = LocalDateTime.now().minusDays(1),
                urlPoster = null,
            ),
        )
        concertPostRepository.saveAndFlush(
            ConcertPost.create(concert, user, "첫 번째 게시글", "첫 번째 게시글 내용", rating = 5, reviewType = ReviewType.REVIEW),
        )

        assertThatThrownBy {
            concertPostRepository.saveAndFlush(
                ConcertPost.create(concert, user, "두 번째 게시글", "두 번째 게시글 내용", rating = 5, reviewType = ReviewType.REVIEW),
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
