package com.back.domain.post.service

import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.entity.ReviewType
import com.back.domain.post.event.ConcertReviewsUpdatedEvent
import com.back.domain.post.repository.ConcertPostRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConcertPostCommandService(
    private val concertPostRepository: ConcertPostRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun save(post: ConcertPost): ConcertPost {
        val saved = concertPostRepository.saveAndFlush(post)
        if (saved.reviewType == ReviewType.REVIEW) {
            eventPublisher.publishEvent(ConcertReviewsUpdatedEvent(saved.concert.concertIdOrThrow))
        }
        return saved
    }
}
