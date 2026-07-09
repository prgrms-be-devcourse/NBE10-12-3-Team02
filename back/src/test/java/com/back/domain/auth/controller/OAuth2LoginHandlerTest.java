package com.back.domain.auth.controller;

import com.back.domain.auth.dto.TokenResponse;
import com.back.domain.auth.service.AuthService;
import com.back.domain.user.entity.LoginType;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.requestcontext.RequestContext;
import com.back.global.security.oauth2.info.GoogleOAuth2UserInfo;
import com.back.global.security.oauth2.info.KakaoOAuth2UserInfo;
import com.back.global.security.oauth2.loginhandler.OAuth2LoginFailureHandler;
import com.back.global.security.oauth2.loginhandler.OAuth2LoginSuccessHandler;
import com.back.global.security.oauth2.loginhandler.OAuth2RedirectHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OAuth2LoginHandlerTest {

    private final OAuth2RedirectHandler redirectHandler = new OAuth2RedirectHandler();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(redirectHandler, "frontCallbackUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(redirectHandler, "frontLoginUrl", "http://localhost:3000/login");
    }

    @Test
    @DisplayName("카카오 OAuth2 사용자 정보 파싱")
    void t1() {
        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(Map.of(
                "id", 12345L,
                "kakao_account", Map.of(
                        "email", "kakao@test.com",
                        "profile", Map.of("nickname", "카카오유저")
                )
        ));

        assertThat(userInfo.getProviderId()).isEqualTo("12345");
        assertThat(userInfo.getEmail()).isEqualTo("kakao@test.com");
        assertThat(userInfo.getName()).isEqualTo("카카오유저");
    }

    @Test
    @DisplayName("구글 OAuth2 사용자 정보 파싱")
    void t2() {
        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(Map.of(
                "sub", "google-sub",
                "email", "google@test.com",
                "name", "구글유저"
        ));

        assertThat(userInfo.getProviderId()).isEqualTo("google-sub");
        assertThat(userInfo.getEmail()).isEqualTo("google@test.com");
        assertThat(userInfo.getName()).isEqualTo("구글유저");
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 시 토큰 발급, refreshToken 쿠키 설정, accessToken fragment redirect")
    void t3() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        AuthService authService = mock(AuthService.class);
        RequestContext requestContext = mock(RequestContext.class);
        OAuth2LoginSuccessHandler successHandler = new OAuth2LoginSuccessHandler(
                userRepository,
                authService,
                requestContext,
                redirectHandler
        );

        User user = User.create(
                "GOOGLE_google-sub",
                "google@test.com",
                "encoded-password",
                "구글유저",
                LoginType.GOOGLE
        );

        when(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(authService.issueTokens(user)).thenReturn(new TokenResponse("access-token", "refresh-token"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(
                mock(HttpServletRequest.class),
                response,
                authentication(oAuth2User(Map.of("userId", 1L)))
        );

        verify(requestContext).setCookie("refreshToken", "refresh-token", "/api/v1/auth");
        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000#accessToken=access-token");
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 핸들러 - userId 누락 시 실패 redirect")
    void t4() throws Exception {
        OAuth2LoginSuccessHandler successHandler = successHandler(
                mock(UserRepository.class),
                mock(AuthService.class),
                mock(RequestContext.class)
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(
                mock(HttpServletRequest.class),
                response,
                authentication(oAuth2User(Map.of("email", "google@test.com")))
        );

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/login?error=oauth2_user_id_missing");
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 핸들러 - userId 형식 오류 시 실패 redirect")
    void t5() throws Exception {
        OAuth2LoginSuccessHandler successHandler = successHandler(
                mock(UserRepository.class),
                mock(AuthService.class),
                mock(RequestContext.class)
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(
                mock(HttpServletRequest.class),
                response,
                authentication(oAuth2User(Map.of("userId", "invalid-user-id")))
        );

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/login?error=oauth2_user_id_invalid");
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 핸들러 - 회원 조회 실패 시 실패 redirect")
    void t6() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        OAuth2LoginSuccessHandler successHandler = successHandler(
                userRepository,
                mock(AuthService.class),
                mock(RequestContext.class)
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(
                mock(HttpServletRequest.class),
                response,
                authentication(oAuth2User(Map.of("userId", 1L)))
        );

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/login?error=oauth2_user_not_found");
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 핸들러 - 토큰 발급 실패 시 실패 redirect")
    void t7() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        AuthService authService = mock(AuthService.class);
        RequestContext requestContext = mock(RequestContext.class);
        User user = User.create("GOOGLE_google-sub", "google@test.com", "encoded-password", "구글유저", LoginType.GOOGLE);

        when(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(authService.issueTokens(user)).thenThrow(new RuntimeException("redis down"));

        OAuth2LoginSuccessHandler successHandler = successHandler(userRepository, authService, requestContext);
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(
                mock(HttpServletRequest.class),
                response,
                authentication(oAuth2User(Map.of("userId", 1L)))
        );

        verify(requestContext, never()).setCookie(anyString(), anyString(), anyString());
        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/login?error=oauth2_token_issue_failed");
    }

    @Test
    @DisplayName("OAuth2 로그인 실패 시 OAuth2 에러 코드로 redirect")
    void t8() throws Exception {
        OAuth2LoginFailureHandler failureHandler = new OAuth2LoginFailureHandler(redirectHandler);
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                mock(HttpServletRequest.class),
                response,
                new OAuth2AuthenticationException("oauth2_email_already_exists")
        );

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/login?error=oauth2_email_already_exists");
    }

    private OAuth2LoginSuccessHandler successHandler(
            UserRepository userRepository,
            AuthService authService,
            RequestContext requestContext
    ) {
        return new OAuth2LoginSuccessHandler(userRepository, authService, requestContext, redirectHandler);
    }

    private Authentication authentication(OAuth2User oAuth2User) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        return authentication;
    }

    private OAuth2User oAuth2User(Map<String, Object> attributes) {
        return new DefaultOAuth2User(List.of(), attributes, attributes.containsKey("userId") ? "userId" : "email");
    }
}
