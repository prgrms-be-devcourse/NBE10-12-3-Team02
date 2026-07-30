package com.back.domain.auth.repository

import com.back.domain.auth.entity.UserSocialAuth
import com.back.domain.user.constant.LoginType
import org.springframework.data.jpa.repository.JpaRepository

interface UserSocialAuthRepository : JpaRepository<UserSocialAuth, Long> {
    fun findByUserUserId(userId: Long): UserSocialAuth?

    fun findByProviderAndProviderId(
        provider: LoginType,
        providerId: String,
    ): UserSocialAuth?

    fun existsByUserUserId(userId: Long): Boolean

    fun existsByProviderAndProviderId(
        provider: LoginType,
        providerId: String,
    ): Boolean

    fun deleteByUserUserId(userId: Long): Long
}
