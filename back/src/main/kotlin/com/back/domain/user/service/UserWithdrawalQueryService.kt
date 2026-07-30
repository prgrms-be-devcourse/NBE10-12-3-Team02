package com.back.domain.user.service

import com.back.domain.auth.repository.UserSocialAuthRepository
import com.back.domain.user.dto.UserWithdrawalTarget
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserWithdrawalQueryService(
    private val userRepository: UserRepository,
    private val userSocialAuthRepository: UserSocialAuthRepository,
) {
    @Transactional(readOnly = true)
    fun getWithdrawalTarget(userId: Long): UserWithdrawalTarget {
        userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND_OR_DELETED)

        val socialAuth = userSocialAuthRepository.findByUserUserId(userId)
        return UserWithdrawalTarget(
            userId = userId,
            provider = socialAuth?.provider,
            providerId = socialAuth?.providerId,
            oauthRefreshToken = socialAuth?.oauthRefreshToken,
        )
    }
}
