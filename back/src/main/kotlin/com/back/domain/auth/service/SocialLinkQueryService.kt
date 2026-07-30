package com.back.domain.auth.service

import com.back.domain.auth.dto.SocialUnlinkTarget
import com.back.domain.auth.repository.UserSocialAuthRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialLinkQueryService(
    private val userRepository: UserRepository,
    private val userSocialAuthRepository: UserSocialAuthRepository,
) {
    @Transactional(readOnly = true)
    fun getUnlinkTarget(userId: Long): SocialUnlinkTarget {
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        if (user.loginType != LoginType.NORMAL) {
            throw ServiceException(ErrorCode.OAUTH_UNLINK_NOT_ALLOWED)
        }

        val socialAuth = userSocialAuthRepository.findByUserUserId(userId)
            ?: throw ServiceException(ErrorCode.OAUTH_ACCOUNT_NOT_LINKED)

        return SocialUnlinkTarget(
            userId = userId,
            provider = socialAuth.provider,
            providerId = socialAuth.providerId,
            oauthRefreshToken = socialAuth.oauthRefreshToken,
        )
    }
}
