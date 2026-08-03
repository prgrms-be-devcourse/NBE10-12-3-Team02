package com.back.domain.post.controller

import com.back.domain.post.dto.ConcertPostResponse
import com.back.domain.post.dto.EligibleConcertResponse
import com.back.domain.post.dto.PostLikeStatusResponse
import com.back.domain.post.dto.PostBookmarkStatusResponse
import com.back.domain.post.entity.ReviewType
import com.back.domain.post.service.ConcertPostService
import com.back.domain.post.service.PostBookmarkService
import com.back.domain.post.service.PostLikeService
import com.back.global.annotation.ApiV1
import com.back.global.requestcontext.RequestContext
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ApiV1
@RestController
@RequestMapping("/posts")
@Tag(name = "Post", description = "Post Board API")
class PostController(
    private val concertPostService: ConcertPostService,
    private val postLikeService: PostLikeService,
    private val postBookmarkService: PostBookmarkService,
    private val requestContext: RequestContext
) {

    @GetMapping
    @Operation(summary = "전체 게시글 피드 조회", description = "모든 콘서트 게시글을 최신순으로 전체 조회")
    fun getAllPosts(): RsData<List<ConcertPostResponse>> {
        val currentUserId = requestContext.actor?.id
        val data = concertPostService.getAllPosts(currentUserId)
        return RsData("200-1", "게시글 목록 조회 성공", data)
    }

    @GetMapping("/me")
    @Operation(summary = "내가 작성한 게시글 목록 조회", description = "마이페이지에서 내가 작성한 게시글을 최신순으로 페이징 조회")
    fun getMyPosts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<Page<ConcertPostResponse>> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val pageable = PageRequest.of(page, size)
        val data = concertPostService.getMyPosts(actor.id, pageable)
        return RsData("200-1", "내 게시글 목록 조회 성공", data)
    }

    @GetMapping("/{postId}")
    @Operation(summary = "게시글 단건 조회", description = "postId로 게시글 상세 조회 (비인증 허용)")
    fun getPost(@PathVariable postId: Long): RsData<ConcertPostResponse> {
        val currentUserId = requestContext.actor?.id
        val data = concertPostService.getDetail(postId, currentUserId)
        return RsData("200-1", "게시글 조회 성공", data)
    }

    @PutMapping("/{postId}/likes")
    @Operation(summary = "게시글 좋아요 등록", description = "게시글에 좋아요를 등록합니다.")
    fun likePost(@PathVariable postId: Long): RsData<PostLikeStatusResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        return RsData(
            "200-1",
            "게시글 좋아요가 등록되었습니다.",
            postLikeService.like(postId, actor.id),
        )
    }

    @DeleteMapping("/{postId}/likes")
    @Operation(summary = "게시글 좋아요 취소", description = "게시글 좋아요를 취소합니다.")
    fun unlikePost(@PathVariable postId: Long): RsData<PostLikeStatusResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        return RsData(
            "200-1",
            "게시글 좋아요가 취소되었습니다.",
            postLikeService.unlike(postId, actor.id),
        )
    }

    @PutMapping("/{postId}/bookmarks")
    @Operation(summary = "게시글 북마크 등록", description = "게시글을 내 북마크에 등록합니다.")
    fun bookmarkPost(@PathVariable postId: Long): RsData<PostBookmarkStatusResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        return RsData(
            "200-1",
            "게시글이 북마크에 등록되었습니다.",
            postBookmarkService.bookmark(postId, actor.id),
        )
    }

    @DeleteMapping("/{postId}/bookmarks")
    @Operation(summary = "게시글 북마크 취소", description = "게시글을 내 북마크에서 제거합니다.")
    fun unbookmarkPost(@PathVariable postId: Long): RsData<PostBookmarkStatusResponse> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        return RsData(
            "200-1",
            "게시글 북마크가 취소되었습니다.",
            postBookmarkService.unbookmark(postId, actor.id),
        )
    }

    @GetMapping("/eligible-concerts")
    @Operation(summary = "게시글 작성 가능 콘서트 목록", description = "현재 로그인 유저가 게시글을 쓸 수 있는 콘서트 목록")
    fun getEligibleConcerts(@RequestParam reviewType: ReviewType): RsData<List<EligibleConcertResponse>> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = concertPostService.getEligibleConcerts(actor.id, reviewType)
        return RsData("200-1", "게시글 작성 가능 콘서트 목록 조회 성공", data)
    }
}
