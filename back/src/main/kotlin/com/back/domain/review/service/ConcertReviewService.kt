package com.back.domain.review.service

import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.review.dto.ConcertReviewCreateRequest
import com.back.domain.review.dto.ConcertReviewResponse
import com.back.domain.review.dto.ConcertReviewUpdateRequest
import com.back.domain.review.dto.EligibleConcertResponse
import com.back.domain.review.entity.ConcertReview
import com.back.domain.review.repository.ConcertReviewRepository
import com.back.domain.ticket.repository.TicketRepository
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ConcertReviewService(
    private val concertReviewRepository: ConcertReviewRepository,
    private val concertRepository: ConcertRepository,
    private val userRepository: UserRepository,
    private val ticketRepository: TicketRepository
) {

    @Transactional
    fun create(concertId: Long, userId: Long, request: ConcertReviewCreateRequest): ConcertReviewResponse {
        val concert = concertRepository.findById(concertId).orElseThrow {
            ServiceException(ErrorCode.CONCERT_NOT_FOUND)
        }

        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        val now = LocalDateTime.now()
        val hasPastTicket = ticketRepository
            .existsByUser_UserIdAndSchedule_Concert_ConcertIdAndSchedule_ScheduleDateBeforeAndIsValidTrue(userId, concertId, now)
        if (!hasPastTicket) {
            throw ServiceException(ErrorCode.REVIEW_NOT_ELIGIBLE)
        }

        val sixMonthsAgo = now.minusMonths(6)
        val hasRecentTicket = ticketRepository
            .existsByUser_UserIdAndSchedule_Concert_ConcertIdAndSchedule_ScheduleDateBetweenAndIsValidTrue(userId, concertId, sixMonthsAgo, now)
        if (!hasRecentTicket) {
            throw ServiceException(ErrorCode.REVIEW_PERIOD_EXPIRED)
        }

        if (concertReviewRepository.existsByConcert_ConcertIdAndUser_UserId(concertId, userId)) {
            throw ServiceException(ErrorCode.REVIEW_ALREADY_EXISTS)
        }

        val review = ConcertReview.create(concert, user, request.title, request.content)
        val saved = try {
            concertReviewRepository.saveAndFlush(review)
        } catch (e: DataIntegrityViolationException) {
            throw ServiceException(ErrorCode.REVIEW_ALREADY_EXISTS)
        }
        return ConcertReviewResponse.of(saved, userId)
    }

    fun getList(concertId: Long, currentUserId: Long?): List<ConcertReviewResponse> {
        if (!concertRepository.existsById(concertId)) {
            throw ServiceException(ErrorCode.CONCERT_NOT_FOUND)
        }
        return concertReviewRepository.findAllByConcertIdWithUser(concertId)
            .map { ConcertReviewResponse.of(it, currentUserId) }
    }

    fun getDetail(reviewId: Long, currentUserId: Long?): ConcertReviewResponse {
        val review = concertReviewRepository.findById(reviewId).orElseThrow {
            ServiceException(ErrorCode.REVIEW_NOT_FOUND)
        }
        return ConcertReviewResponse.of(review, currentUserId)
    }

    fun getDetail(concertId: Long, reviewId: Long, currentUserId: Long?): ConcertReviewResponse {
        val review = findByConcertIdAndReviewId(concertId, reviewId)
        return ConcertReviewResponse.of(review, currentUserId)
    }

    @Transactional
    fun update(
        concertId: Long,
        reviewId: Long,
        userId: Long,
        request: ConcertReviewUpdateRequest,
    ): ConcertReviewResponse {
        val review = findByConcertIdAndReviewId(concertId, reviewId)
        if (review.user.userId != userId) {
            throw ServiceException(ErrorCode.REVIEW_FORBIDDEN)
        }
        review.update(request.title, request.content)
        return ConcertReviewResponse.of(review, userId)
    }

    fun getAllReviews(currentUserId: Long?): List<ConcertReviewResponse> {
        return concertReviewRepository.findAllWithConcertAndUser()
            .map { ConcertReviewResponse.of(it, currentUserId) }
    }

    fun getEligibleConcerts(userId: Long): List<EligibleConcertResponse> {
        val now = LocalDateTime.now()
        val sixMonthsAgo = now.minusMonths(6)
        return concertReviewRepository.findEligibleConcertsForUser(userId, sixMonthsAgo, now)
            .map { EligibleConcertResponse.of(it) }
    }

    @Transactional
    fun delete(concertId: Long, reviewId: Long, userId: Long) {
        val review = findByConcertIdAndReviewId(concertId, reviewId)
        if (review.user.userId != userId) {
            throw ServiceException(ErrorCode.REVIEW_FORBIDDEN)
        }
        concertReviewRepository.delete(review)
    }

    private fun findByConcertIdAndReviewId(concertId: Long, reviewId: Long): ConcertReview =
        concertReviewRepository.findByReviewIdAndConcertConcertId(reviewId, concertId)
            ?: throw ServiceException(ErrorCode.REVIEW_NOT_FOUND)
}
