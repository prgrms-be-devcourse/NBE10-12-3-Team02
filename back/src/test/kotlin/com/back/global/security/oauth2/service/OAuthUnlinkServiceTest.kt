package com.back.global.security.oauth2.service

import com.back.domain.user.constant.LoginType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.web.client.RestTemplate

class OAuthUnlinkServiceTest {
    private val restTemplate = mock(RestTemplate::class.java)
    private val service = OAuthUnlinkService(
        restTemplate = restTemplate,
        kakaoClientId = "kakao-client-id",
        kakaoClientSecret = "kakao-client-secret",
        naverClientId = "naver-client-id",
        naverClientSecret = "naver-client-secret",
    )

    @Test
    @DisplayName("Provider Refresh Token이 없으면 외부 요청 없이 내부 해제를 허용한다")
    fun t1() {
        val result = service.unlink(LoginType.KAKAO, null)

        assertThat(result).isEqualTo(OAuthUnlinkResult.LOCAL_ONLY)
        verifyNoInteractions(restTemplate)
    }

    @Test
    @DisplayName("Provider Refresh Token이 빈 문자열이어도 외부 요청 없이 내부 해제를 허용한다")
    fun t2() {
        val result = service.unlink(LoginType.GOOGLE, " ")

        assertThat(result).isEqualTo(OAuthUnlinkResult.LOCAL_ONLY)
        verifyNoInteractions(restTemplate)
    }

    @Test
    @DisplayName("NORMAL 타입은 Provider 해제 대상이 아니므로 실패 처리한다")
    fun t3() {
        val result = service.unlink(LoginType.NORMAL, null)

        assertThat(result).isEqualTo(OAuthUnlinkResult.FAILED)
        verifyNoInteractions(restTemplate)
    }
}
