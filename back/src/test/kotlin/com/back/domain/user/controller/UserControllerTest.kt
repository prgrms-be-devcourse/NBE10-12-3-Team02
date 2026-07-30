package com.back.domain.user.controller

import com.back.domain.user.constant.LoginType
import com.back.domain.auth.service.EmailVerificationService
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.RedisTestConfig
import com.back.global.security.SecurityUser
import com.back.global.security.jwt.JwtTokenProvider
import com.back.global.security.jwt.payload.AccessTokenPayload
import com.back.global.security.jwt.repository.BlacklistRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RedisTestConfig::class)
class UserControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val objectMapper = ObjectMapper()
    private lateinit var userEntity: User
    private lateinit var securityUser: SecurityUser

    @MockitoBean
    private lateinit var blacklistRepository: BlacklistRepository

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var emailVerificationService: EmailVerificationService

    @BeforeEach
    fun setUp() {
        userEntity = userRepository.save(
            User.create(
                "testuser",
                "test@naver.com",
                passwordEncoder.encode("q1w2e3r4")!!,
                "홍길동",
                LoginType.NORMAL
            )
        )

        val userId = userEntity.userId ?: throw IllegalStateException("User ID null")
        securityUser = SecurityUser(userId, userEntity.name)

        `when`(jwtTokenProvider.parseAccessToken(anyString()))
            .thenReturn(AccessTokenPayload(userId, userEntity.name))

        `when`(jwtTokenProvider.getRemainingSeconds(anyString()))
            .thenReturn(600L)
        `when`(emailVerificationService.reserveVerification(anyString(), anyString()))
            .thenReturn("test-reservation-id")
    }

    @Test
    @DisplayName("회원가입 성공")
    fun t1() {
        mockMvc.perform(
            post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "id" to "testuser1",
                            "email" to "test1@naver.com",
                            "password" to "q1w2e3r4",
                            "name" to "홍길동",
                            "verificationToken" to "verification-token"
                        )
                    )
                )
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("회원가입이 완료되었습니다."))
            .andExpect(jsonPath("$.data.userId").exists())
            .andExpect(jsonPath("$.data.loginType").value("NORMAL"))
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 중복")
    fun t2() {
        mockMvc.perform(
            post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "id" to "testuser",
                            "email" to "other@naver.com",
                            "password" to "q1w2e3r4",
                            "name" to "김철수",
                            "verificationToken" to "verification-token"
                        )
                    )
                )
        )
            .andDo(print())
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409-1"))
            .andExpect(jsonPath("$.msg").value("이미 사용 중인 아이디입니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    fun t3() {
        mockMvc.perform(
            post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "id" to "otheruser",
                            "email" to "test@naver.com",
                            "password" to "q1w2e3r4",
                            "name" to "김철수",
                            "verificationToken" to "verification-token"
                        )
                    )
                )
        )
            .andDo(print())
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409-2"))
            .andExpect(jsonPath("$.msg").value("이미 사용 중인 이메일입니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 8자 미만")
    fun t4() {
        mockMvc.perform(
            post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "id" to "testuser",
                            "email" to "test@naver.com",
                            "password" to "q1w2e3",
                            "name" to "홍길동",
                            "verificationToken" to "verification-token"
                        )
                    )
                )
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    fun t5() {
        mockMvc.perform(
            patch("/api/v1/users/withdraw")
                .with(user(securityUser))
                .header("Authorization", "Bearer test-access-token")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("회원 탈퇴가 정상적으로 완료되었습니다."))
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 존재하지 않는 회원")
    fun t6() {
        `when`(jwtTokenProvider.parseAccessToken(anyString()))
            .thenReturn(AccessTokenPayload(999L, "없는사용자"))

        mockMvc.perform(
            patch("/api/v1/users/withdraw")
                .with(user(SecurityUser(999L, "없는사용자")))
                .header("Authorization", "Bearer test-access-token")
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-2"))
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 이미 탈퇴한 회원")
    fun t7() {
        userEntity.withdraw()
        userRepository.saveAndFlush(userEntity)

        mockMvc.perform(
            patch("/api/v1/users/withdraw")
                .with(user(securityUser))
                .header("Authorization", "Bearer test-access-token")
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-2"))
    }

    @Test
    @DisplayName("마이페이지 조회 성공")
    fun t8() {
        mockMvc.perform(
            get("/api/v1/users/me")
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("마이페이지 조회 성공"))
            .andExpect(jsonPath("$.data.name").value("홍길동"))
            .andExpect(jsonPath("$.data.ticketGroups").isArray)
    }

    @Test
    @DisplayName("마이페이지 조회 실패 - 존재하지 않는 회원")
    fun t9() {
        mockMvc.perform(
            get("/api/v1/users/me")
                .with(user(SecurityUser(999L, "없는사용자")))
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @DisplayName("마이페이지 수정 성공 - 이름만 변경")
    fun t10() {
        mockMvc.perform(
            patch("/api/v1/users/me")
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("name" to "김철수")))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("마이페이지 수정 성공"))

        val updated = userRepository.findById(userEntity.userId!!).orElseThrow()
        assertThat(updated.name).isEqualTo("김철수")
        assertThat(updated.email).isEqualTo("test@naver.com")
    }

    @Test
    @DisplayName("마이페이지 수정 실패 - 이메일은 변경 불가")
    fun t11() {
        userRepository.save(
            User.create("otheruser", "other@naver.com", passwordEncoder.encode("q1w2e3r4")!!, "김철수", LoginType.NORMAL)
        )

        mockMvc.perform(
            patch("/api/v1/users/me")
                .with(user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("email" to "other@naver.com")))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-12"))
    }

    @Test
    @DisplayName("마이페이지 수정 실패 - 존재하지 않는 회원")
    fun t12() {
        mockMvc.perform(
            patch("/api/v1/users/me")
                .with(user(SecurityUser(999L, "없는사용자")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("name" to "김철수")))
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @DisplayName("아이디 중복확인 성공")
    fun t13() {
        mockMvc.perform(
            get("/api/v1/users/check-id")
                .param("id", "newuser123")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("사용 가능한 아이디입니다."))
    }

    @Test
    @DisplayName("아이디 중복확인 실패 - 중복")
    fun t14() {
        userRepository.save(
            User.create("existuser", "exist@naver.com", passwordEncoder.encode("q1w2e3r4")!!, "홍길동", LoginType.NORMAL)
        )

        mockMvc.perform(
            get("/api/v1/users/check-id")
                .param("id", "existuser")
        )
            .andDo(print())
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409-1"))
            .andExpect(jsonPath("$.msg").value("이미 사용 중인 아이디입니다."))
    }

    @Test
    @DisplayName("프로필 사진 업로드 성공")
    fun t15() {
        val file = MockMultipartFile(
            "file", "profile.png", "image/png", "dummy-image-bytes".toByteArray()
        )

        mockMvc.perform(
            multipart("/api/v1/users/me/profile-image")
                .file(file)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("프로필 사진이 변경되었습니다."))
            .andExpect(jsonPath("$.data.profileImageUrl").exists())

        val updated = userRepository.findById(userEntity.userId!!).orElseThrow()
        assertThat(updated.profileImgUrl).isNotNull()
    }

    @Test
    @DisplayName("프로필 사진 업로드 실패 - 허용되지 않는 확장자")
    fun t16() {
        val file = MockMultipartFile(
            "file", "profile.txt", "text/plain", "not-an-image".toByteArray()
        )

        mockMvc.perform(
            multipart("/api/v1/users/me/profile-image")
                .file(file)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-10"))
    }

    @Test
    @DisplayName("프로필 사진 업로드 실패 - 빈 파일")
    fun t17() {
        val file = MockMultipartFile(
            "file", "empty.png", "image/png", ByteArray(0)
        )

        mockMvc.perform(
            multipart("/api/v1/users/me/profile-image")
                .file(file)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-7"))
    }

    @Test
    @DisplayName("프로필 사진 삭제 성공")
    fun t18() {
        val file = MockMultipartFile(
            "file", "profile.png", "image/png", "dummy-image-bytes".toByteArray()
        )
        mockMvc.perform(
            multipart("/api/v1/users/me/profile-image")
                .file(file)
                .with(user(securityUser))
        )

        mockMvc.perform(
            delete("/api/v1/users/me/profile-image")
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("프로필 사진이 기본 이미지로 변경되었습니다."))

        val updated = userRepository.findById(userEntity.userId!!).orElseThrow()
        assertThat(updated.profileImgUrl).isNull()
    }

    @Test
    @DisplayName("프로필 이미지 리다이렉트 - 사진 없으면 기본 이미지로 리다이렉트")
    fun t19() {
        mockMvc.perform(
            get("/api/v1/users/${userEntity.userId}/redirectToProfileImg")
        )
            .andDo(print())
            .andExpect(status().isFound)
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Cache-Control", "max-age=1200, public, immutable"))
    }

    @Test
    @DisplayName("프로필 이미지 리다이렉트 - 존재하지 않는 회원이면 404")
    fun t20() {
        mockMvc.perform(
            get("/api/v1/users/999/redirectToProfileImg")
        )
            .andDo(print())
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 인증 토큰이 유효하지 않음")
    fun t21() {
        `when`(
            emailVerificationService.reserveVerification(
                "unverified@example.com",
                "invalid-verification-token",
            )
        ).thenReturn(null)

        mockMvc.perform(
            post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "id" to "unverified-user",
                            "email" to "unverified@example.com",
                            "password" to "q1w2e3r4",
                            "name" to "홍길동",
                            "verificationToken" to "invalid-verification-token",
                        )
                    )
                )
        )
            .andDo(print())
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-12"))
            .andExpect(jsonPath("$.msg").value("이메일 인증이 필요합니다."))
    }

    @Test
    @DisplayName("소셜 계정이 연결되지 않은 회원의 연동 상태를 조회한다")
    fun t22() {
        mockMvc.perform(
            get("/api/v1/users/me/social-links")
                .with(user(securityUser))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.linked").value(false))
            .andExpect(jsonPath("$.data.provider").doesNotExist())
    }

    @Test
    @DisplayName("소셜 계정 연동 시작 시 일회성 쿠키를 발급하고 Provider 인증 경로로 이동한다")
    fun t23() {
        mockMvc.perform(
            get("/api/v1/users/me/social-links/kakao")
                .with(user(securityUser))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.authorizationUrl")
            .value("/oauth2/authorization/kakao"))
            .andExpect(cookie().exists("oauth2_link_intent"))
            .andExpect(cookie().httpOnly("oauth2_link_intent", true))
            .andExpect(cookie().maxAge("oauth2_link_intent", 300))
    }

    @Test
    @DisplayName("지원하지 않는 Provider로 소셜 계정 연동을 시작할 수 없다")
    fun t24() {
        mockMvc.perform(
            get("/api/v1/users/me/social-links/unsupported")
                .with(user(securityUser))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-11"))
    }

    @Test
    @DisplayName("연결된 소셜 계정이 없으면 연동 해제를 거절한다")
    fun t25() {
        mockMvc.perform(
            delete("/api/v1/users/me/social-links")
                .with(user(securityUser))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-9"))
    }
}
