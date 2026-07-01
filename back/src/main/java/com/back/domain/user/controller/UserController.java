package com.back.domain.user.controller;

import com.back.domain.user.dto.MyPageResponse;
import com.back.domain.user.dto.SignupRequest;
import com.back.domain.user.dto.SignupResponse;
import com.back.domain.user.dto.UpdateMyPageRequest;
import com.back.domain.user.service.UserService;
import com.back.global.annotation.ApiV1;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "회원가입", description = "회원가입 API")
    public RsData<SignupResponse> signup(@RequestBody @Valid SignupRequest request) {
        return new RsData<>("200-1", "회원가입이 완료되었습니다.", userService.signup(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴 API")
    public RsData<Void> withdraw(
            @PathVariable Long id,
            @RequestHeader("X-Impersonate-User-Id") Long userId) {
        userService.withdraw(id, userId);
        return new RsData<>("200-1", "회원 탈퇴가 정상적으로 완료되었습니다.", null);
    }

    @GetMapping("/me/{userId}")
    @Operation(summary = "마이페이지 조회", description = "마이페이지 조회 API")
    public RsData<MyPageResponse> getMyPage(@PathVariable Long userId) {
        return new RsData<>("200-1", "마이페이지 조회 성공", userService.getMyPage(userId));
    }
    @PatchMapping("/me/{userId}")
    @Operation(summary = "마이페이지 수정", description = "마이페이지 수정 API")
    public RsData<Void> updateMyPage(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateMyPageRequest request) {
        userService.updateMyPage(userId, request);
        return new RsData<>("200-1", "마이페이지 수정 성공", null);
    }
}