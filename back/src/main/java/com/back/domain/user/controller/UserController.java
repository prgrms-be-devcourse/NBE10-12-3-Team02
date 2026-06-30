package com.back.domain.user.controller;

import com.back.domain.user.dto.SignupRequest;
import com.back.domain.user.dto.SignupResponse;
import com.back.domain.user.service.UserService;
import com.back.global.annotation.ApiV1;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@ApiV1
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User API")
public class UserController {

    private final UserService userService;

    @PostMapping("/signin")
    public RsData<SignupResponse> signup(@RequestBody @Valid SignupRequest request) {
        return new RsData<>("200-1", "회원가입이 완료되었습니다.", userService.signup(request));
    }
}