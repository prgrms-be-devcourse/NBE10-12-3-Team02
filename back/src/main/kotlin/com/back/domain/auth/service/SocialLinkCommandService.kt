package com.back.domain.auth.service

import com.back.domain.auth.dto.SocialUnlinkTarget
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialLinkCommandService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun completeUnlink(target: SocialUnlinkTarget) {
        val updatedCount = userRepository.unlinkSocialAccountIfMatches(
            userId = target.userId,
            socialProvider = target.provider,
            socialProviderId = target.providerId,
        )

        if (updatedCount != 1) {
            throw ServiceException(ErrorCode.OAUTH_LINK_STATE_CHANGED)
        }
    }
}
