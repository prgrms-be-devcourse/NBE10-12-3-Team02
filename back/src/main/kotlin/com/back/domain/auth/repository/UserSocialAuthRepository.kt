package com.back.domain.auth.repository

import com.back.domain.auth.entity.UserSocialAuth
import com.back.domain.user.constant.LoginType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        DELETE FROM UserSocialAuth socialAuth
        WHERE socialAuth.user.userId = :userId
          AND socialAuth.provider = :provider
          AND socialAuth.providerId = :providerId
        """,
    )
    fun deleteIfMatches(
        @Param("userId") userId: Long,
        @Param("provider") provider: LoginType,
        @Param("providerId") providerId: String,
    ): Int

    fun deleteByUserUserId(userId: Long): Long
}
