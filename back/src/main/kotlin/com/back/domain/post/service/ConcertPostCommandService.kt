package com.back.domain.post.service

import com.back.domain.post.entity.ConcertPost
import com.back.domain.post.repository.ConcertPostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConcertPostCommandService(
    private val concertPostRepository: ConcertPostRepository,
) {
    @Transactional
    fun save(post: ConcertPost): ConcertPost =
        concertPostRepository.saveAndFlush(post)
}
