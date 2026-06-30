package com.back.domain.user.controller;

import com.back.domain.user.entity.LoginType;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserControllerTest(
            MockMvc mockMvc,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    @DisplayName("회원가입 성공")
    void t1() throws Exception {
        mockMvc.perform(post("/api/v1/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", "testuser",
                                "email", "test@naver.com",
                                "password", "q1w2e3r4",
                                "name", "홍길동"
                        ))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.loginType").value("NORMAL"));
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 중복")
    void t2() throws Exception {
        userRepository.save(User.create("testuser", "test@naver.com",
                passwordEncoder.encode("q1w2e3r4"), "홍길동", LoginType.NORMAL));

        mockMvc.perform(post("/api/v1/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", "testuser",
                                "email", "other@naver.com",
                                "password", "q1w2e3r4",
                                "name", "김철수"
                        ))))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"))
                .andExpect(jsonPath("$.msg").value("이미 사용 중인 아이디입니다."));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void t3() throws Exception {
        userRepository.save(User.create("testuser", "test@naver.com",
                passwordEncoder.encode("q1w2e3r4"), "홍길동", LoginType.NORMAL));

        mockMvc.perform(post("/api/v1/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", "otheruser",
                                "email", "test@naver.com",
                                "password", "q1w2e3r4",
                                "name", "김철수"
                        ))))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-2"))
                .andExpect(jsonPath("$.msg").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 8자 미만")
    void t4() throws Exception {
        mockMvc.perform(post("/api/v1/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", "testuser",
                                "email", "test@naver.com",
                                "password", "q1w2e3",   // 7자
                                "name", "홍길동"
                        ))))
                .andDo(print())
                .andExpect(status().isBadRequest());   // 400
    }
}