package com.back.domain.post.repository

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.PostBookmark
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
            ConcertPost.create(concert, user, "게시글", "게시글 내용")
        )
        postBookmarkRepository.saveAndFlush(PostBookmark.create(post, user))

        assertThatThrownBy {
            postBookmarkRepository.saveAndFlush(PostBookmark.create(post, user))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
