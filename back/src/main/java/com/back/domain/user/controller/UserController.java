package com.back.domain.user.controller;

import com.back.domain.user.dto.MyPageResponse;
import com.back.domain.user.dto.SignupRequest;
import com.back.domain.user.dto.SignupResponse;
import com.back.domain.user.dto.UpdateMyPageRequest;
import com.back.domain.user.service.UserService;
import com.back.global.annotation.ApiV1;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public RsData<Void> withdraw(@PathVariable Long id, @AuthenticationPrincipal SecurityUser securityUser) {
        userService.withdraw(id, securityUser.getId());
        return new RsData<>("200-1", "회원 탈퇴가 정상적으로 완료되었습니다.", null);
    }

    @GetMapping("/me")
    @Operation(summary = "마이페이지 조회", description = "마이페이지 조회 API")
    public RsData<MyPageResponse> getMyPage(@AuthenticationPrincipal SecurityUser securityUser) {
        return new RsData<>("200-1", "마이페이지 조회 성공", userService.getMyPage(securityUser.getId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "마이페이지 수정", description = "마이페이지 수정 API")
    public RsData<Void> updateMyPage(@RequestBody @Valid UpdateMyPageRequest request, @AuthenticationPrincipal SecurityUser securityUser) {
        userService.updateMyPage(securityUser.getId(), request);
        return new RsData<>("200-1", "마이페이지 수정 성공", null);
    }
    @GetMapping("/check-id")
    @Operation(summary = "아이디 중복확인", description = "아이디 중복확인 API")
    public RsData<Void> checkId(@RequestParam String id) {
        userService.checkId(id);
        return new RsData<>("200-1", "사용 가능한 아이디입니다.", null);
    }
}