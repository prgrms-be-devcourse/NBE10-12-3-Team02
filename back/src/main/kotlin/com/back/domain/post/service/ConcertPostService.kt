package com.back.domain.post.service

import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.post.dto.ConcertPostCreateRequest
import com.back.domain.post.dto.ConcertPostResponse
import com.back.domain.post.dto.ConcertPostUpdateRequest
import com.back.domain.post.dto.EligibleConcertResponse
import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.ReviewType
import com.back.domain.post.repository.ConcertPostRepository
import com.back.domain.post.repository.PostBookmarkRepository
import com.back.domain.post.repository.PostLikeRepository
import com.back.domain.ticket.repository.TicketRepository
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ConcertPostService(
    private val concertPostRepository: ConcertPostRepository,
    private val concertRepository: ConcertRepository,
    private val userRepository: UserRepository,
    private val ticketRepository: TicketRepository,
    private val postLikeRepository: PostLikeRepository,
    private val postBookmarkRepository: PostBookmarkRepository,
    private val concertPostCommandService: ConcertPostCommandService,
) {

    fun create(concertId: Long, userId: Long, request: ConcertPostCreateRequest): ConcertPostResponse {
        val concert = concertRepository.findById(concertId).orElseThrow {
            ServiceException(ErrorCode.CONCERT_NOT_FOUND)
        }

        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        val now = LocalDateTime.now()

        when (request.reviewType) {
            ReviewType.REVIEW -> {
                val hasPastTicket = ticketRepository
                    .existsByUser_UserIdAndSchedule_Concert_ConcertIdAndSchedule_ScheduleDateBeforeAndIsValidTrue(userId, concertId, now)
                if (!hasPastTicket) {
                    throw ServiceException(ErrorCode.POST_NOT_ELIGIBLE)
                }

                val sixMonthsAgo = now.minusMonths(6)
                val hasRecentTicket = ticketRepository
                    .existsByUser_UserIdAndSchedule_Concert_ConcertIdAndSchedule_ScheduleDateBetweenAndIsValidTrue(userId, concertId, sixMonthsAgo, now)
                if (!hasRecentTicket) {
                    throw ServiceException(ErrorCode.POST_PERIOD_EXPIRED)
                }
            }
            ReviewType.EXPECTATION -> {
                val hasReservation = ticketRepository
                    .existsByUser_UserIdAndSchedule_Concert_ConcertId(userId, concertId)
                if (!hasReservation) {
                    throw ServiceException(ErrorCode.POST_NOT_ELIGIBLE)
                }
            }
        }

        if (concertPostRepository.existsByConcert_ConcertIdAndUser_UserIdAndReviewType(concertId, userId, request.reviewType)) {
            throw ServiceException(ErrorCode.POST_ALREADY_EXISTS)
        }

        val post = ConcertPost.create(concert, user, request.title, request.content, request.rating, request.reviewType)
        val saved = try {
            concertPostCommandService.save(post)
        } catch (e: DataIntegrityViolationException) {
            throw ServiceException(ErrorCode.POST_ALREADY_EXISTS)
        }
        return toResponse(saved, userId)
    }

    @Transactional(readOnly = true)
    fun getList(concertId: Long, currentUserId: Long?): List<ConcertPostResponse> {
        if (!concertRepository.existsById(concertId)) {
            throw ServiceException(ErrorCode.CONCERT_NOT_FOUND)
        }
        val posts = concertPostRepository.findAllByConcertIdWithUser(concertId)
        return toResponses(posts, currentUserId)
    }

    @Transactional(readOnly = true)
    fun getDetail(postId: Long, currentUserId: Long?): ConcertPostResponse {
        val post = concertPostRepository.findById(postId).orElseThrow {
            ServiceException(ErrorCode.POST_NOT_FOUND)
        }
        return toResponse(post, currentUserId)
    }

    @Transactional(readOnly = true)
    fun getDetail(concertId: Long, postId: Long, currentUserId: Long?): ConcertPostResponse {
        val post = findByConcertIdAndPostId(concertId, postId)
        return toResponse(post, currentUserId)
    }

    @Transactional
    fun update(
        concertId: Long,
        postId: Long,
        userId: Long,
        request: ConcertPostUpdateRequest,
    ): ConcertPostResponse {
        val post = findByConcertIdAndPostId(concertId, postId)
        if (post.user.userId != userId) {
            throw ServiceException(ErrorCode.POST_FORBIDDEN)
        }
        post.update(request.title, request.content, request.rating)
        // reviewType은 생성 시에만 결정되며 수정 불가
        return toResponse(post, userId)
    }

    @Transactional(readOnly = true)
    fun getAllPosts(currentUserId: Long?): List<ConcertPostResponse> {
        val posts = concertPostRepository.findAllWithConcertAndUser()
        return toResponses(posts, currentUserId)
    }

    @Transactional(readOnly = true)
    fun getMyPosts(userId: Long, pageable: Pageable): Page<ConcertPostResponse> {
        val posts = concertPostRepository.findAllByUser_UserId(userId, pageable)
        val responses = toResponses(posts.content, userId)
        return PageImpl(responses, pageable, posts.totalElements)
    }

    @Transactional(readOnly = true)
    fun getEligibleConcerts(userId: Long, reviewType: ReviewType): List<EligibleConcertResponse> {
        val concerts = when (reviewType) {
            ReviewType.REVIEW -> {
                val now = LocalDateTime.now()
                val sixMonthsAgo = now.minusMonths(6)
                concertPostRepository.findEligibleConcertsForReview(userId, sixMonthsAgo, now, reviewType)
            }
            ReviewType.EXPECTATION -> concertPostRepository.findEligibleConcertsForExpectation(userId, reviewType)
        }
        return concerts.map { EligibleConcertResponse.of(it) }
    }

    @Transactional
    fun delete(concertId: Long, postId: Long, userId: Long) {
        val post = findByConcertIdAndPostId(concertId, postId)
        if (post.user.userId != userId) {
            throw ServiceException(ErrorCode.POST_FORBIDDEN)
        }
        postLikeRepository.deleteAllByPostPostId(postId)
        postBookmarkRepository.deleteAllByPostPostId(postId)
        concertPostRepository.delete(post)
    }

    private fun findByConcertIdAndPostId(concertId: Long, postId: Long): ConcertPost =
        concertPostRepository.findByPostIdAndConcertConcertId(postId, concertId)
            ?: throw ServiceException(ErrorCode.POST_NOT_FOUND)

    private fun toResponse(
        post: ConcertPost,
        currentUserId: Long?,
    ): ConcertPostResponse {
        val postId = post.postIdOrThrow
        val isLiked = currentUserId != null &&
            postLikeRepository.existsByPostPostIdAndUserUserId(postId, currentUserId)
        val isBookmarked = currentUserId != null &&
            postBookmarkRepository.existsByPostPostIdAndUserUserId(postId, currentUserId)
        return ConcertPostResponse.of(
            post = post,
            currentUserId = currentUserId,
            likeCount = postLikeRepository.countByPostPostId(postId),
            isLiked = isLiked,
            isBookmarked = isBookmarked,
        )
    }

    private fun toResponses(
        posts: List<ConcertPost>,
        currentUserId: Long?,
    ): List<ConcertPostResponse> {
        if (posts.isEmpty()) {
            return emptyList()
        }

        val postIds = posts.map { it.postIdOrThrow }
        val likeCounts = postLikeRepository.countByPostIds(postIds)
            .associate { it.postId to it.likeCount }
        val likedPostIds = currentUserId
            ?.let { postLikeRepository.findLikedPostIds(postIds, it).toSet() }
            ?: emptySet()
        val bookmarkedPostIds = currentUserId
            ?.let { postBookmarkRepository.findBookmarkedPostIds(postIds, it).toSet() }
            ?: emptySet()

        return posts.map { post ->
            val postId = post.postIdOrThrow
            ConcertPostResponse.of(
                post = post,
                currentUserId = currentUserId,
                likeCount = likeCounts[postId] ?: 0L,
                isLiked = postId in likedPostIds,
                isBookmarked = postId in bookmarkedPostIds,
            )
        }
    }
}
