package com.back.domain.auth.service

import com.back.domain.auth.dto.SocialUnlinkTarget
import com.back.domain.user.constant.LoginType
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SocialLinkCommandServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val service = SocialLinkCommandService(userRepository)

    @Test
    @DisplayName("조회 시점과 같은 소셜 계정이 연결되어 있으면 내부 연동 정보를 제거한다")
    fun t1() {
        val target = unlinkTarget()
        `when`(
            userRepository.unlinkSocialAccountIfMatches(
                USER_ID,
                LoginType.NAVER,
                PROVIDER_ID,
            ),
        ).thenReturn(1)

        service.completeUnlink(target)

        verify(userRepository).unlinkSocialAccountIfMatches(
            USER_ID,
            LoginType.NAVER,
            PROVIDER_ID,
        )
    }

    @Test
    @DisplayName("외부 호출 중 소셜 연동 상태가 바뀌면 내부 정보를 삭제하지 않고 충돌로 처리한다")
    fun t2() {
        val target = unlinkTarget()
        `when`(
            userRepository.unlinkSocialAccountIfMatches(
                USER_ID,
                LoginType.NAVER,
                PROVIDER_ID,
            ),
        ).thenReturn(0)

        assertThatThrownBy { service.completeUnlink(target) }
            .isInstanceOfSatisfying(ServiceException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.OAUTH_LINK_STATE_CHANGED)
            }
    }

    private fun unlinkTarget(): SocialUnlinkTarget =
        SocialUnlinkTarget(
            userId = USER_ID,
            provider = LoginType.NAVER,
            providerId = PROVIDER_ID,
            oauthRefreshToken = OAUTH_REFRESH_TOKEN,
        )

    companion object {
        private const val USER_ID = 1L
        private const val PROVIDER_ID = "provider-id"
        private const val OAUTH_REFRESH_TOKEN = "oauth-refresh-token"
    }
}
