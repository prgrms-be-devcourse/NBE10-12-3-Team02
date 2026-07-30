package com.back.domain.user.controller

import com.back.domain.auth.dto.SocialLinkAuthorizationResponse
import com.back.domain.user.dto.MyPageResponse
import com.back.domain.user.dto.ProfileImageResponse
import com.back.domain.user.dto.SignupRequest
import com.back.domain.user.dto.SignupResponse
import com.back.domain.auth.dto.SocialLinkStatusResponse
import com.back.domain.auth.repository.SocialLinkCookieRepository
import com.back.domain.user.dto.UpdateMyPageRequest
import com.back.domain.user.service.UserService
import com.back.domain.auth.service.SocialLinkService
import com.back.global.annotation.ApiV1
import com.back.global.requestcontext.RequestContext
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.util.concurrent.TimeUnit

@ApiV1
@RestController
@RequestMapping("/users")
@Tag(name = "User", description = "User API")
class UserController(
    private val userService: UserService,
    private val requestContext: RequestContext,
    private val socialLinkService: SocialLinkService,
    private val socialLinkCookieRepository: SocialLinkCookieRepository,
) {

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "회원가입 API")
    fun signup(@RequestBody @Valid request: SignupRequest): RsData<SignupResponse> {
        return RsData("200-1", "회원가입이 완료되었습니다.", userService.signup(request))
    }

    @PatchMapping("/withdraw")
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴 API")
    fun withdraw(@RequestHeader("Authorization") authorization: String): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        userService.withdraw(actor.id, authorization)
        requestContext.deleteCookie("refreshToken", "/api/v1/auth")
        return RsData("200-1", "회원 탈퇴가 정상적으로 완료되었습니다.", null)
    }

    @GetMapping("/me")
    @Operation(summary = "마이페이지 조회", description = "마이페이지 조회 API")
    fun getMyPage(): RsData<MyPageResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        return RsData("200-1", "마이페이지 조회 성공", userService.getMyPage(actor.id))
    }

    @GetMapping("/me/social-links")
    @Operation(summary = "소셜 계정 연동 상태 조회")
    fun getSocialLinkStatus(): RsData<SocialLinkStatusResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        return RsData(
            "200-1",
            "소셜 계정 연동 상태 조회 성공",
            socialLinkService.getStatus(actor.id),
        )
    }

    @GetMapping("/me/social-links/{provider}")
    @Operation(summary = "소셜 계정 연동 시작")
    fun startSocialLink(@PathVariable provider: String): RsData<SocialLinkAuthorizationResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val result = socialLinkService.start(actor.id, provider)
        socialLinkCookieRepository.save(result.intentId)

        return RsData(
            "200-1",
            "소셜 계정 연동 요청이 생성되었습니다.",
            SocialLinkAuthorizationResponse(result.authorizationPath)
        )
    }

    @DeleteMapping("/me/social-links")
    @Operation(summary = "소셜 계정 연동 해제")
    fun unlinkSocialAccount(): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        socialLinkService.unlink(actor.id)
        return RsData("200-1", "소셜 계정 연동이 해제되었습니다.", null)
    }

    @PatchMapping("/me")
    @Operation(summary = "마이페이지 수정", description = "마이페이지 수정 API")
    fun updateMyPage(@RequestBody @Valid request: UpdateMyPageRequest): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        userService.updateMyPage(actor.id, request)
        return RsData("200-1", "마이페이지 수정 성공", null)
    }

    @GetMapping("/check-id")
    @Operation(summary = "아이디 중복확인", description = "아이디 중복확인 API")
    fun checkId(@RequestParam id: String): RsData<Void> {
        userService.checkId(id)
        return RsData("200-1", "사용 가능한 아이디입니다.", null)
    }

    @PostMapping("/me/profile-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "프로필 사진 업로드/변경", description = "프로필 사진 업로드/변경 API")
    fun uploadProfileImage(@RequestParam file: MultipartFile): RsData<ProfileImageResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val url = userService.updateProfileImg(actor.id, file)
        return RsData("200-1", "프로필 사진이 변경되었습니다.", ProfileImageResponse(url))
    }

    @DeleteMapping("/me/profile-image")
    @Operation(summary = "프로필 사진 삭제", description = "프로필 사진을 기본 이미지로 되돌리는 API")
    fun deleteProfileImage(): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        userService.deleteProfileImg(actor.id)
        return RsData("200-1", "프로필 사진이 기본 이미지로 변경되었습니다.", null)
    }

    @GetMapping("/{id}/redirectToProfileImg")
    @ResponseStatus(HttpStatus.FOUND)
    @Operation(summary = "프로필 이미지 리다이렉트(브라우저 캐시 20분)")
    fun redirectToProfileImg(@PathVariable id: Long): ResponseEntity<Void> {
        val redirectUrl = userService.getProfileImgRedirectUrl(id)
        val cacheControl = CacheControl.maxAge(20, TimeUnit.MINUTES).cachePublic().immutable()
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(redirectUrl))
            .cacheControl(cacheControl)
            .build()
    }
}
