package com.back.domain.auth.repository

import com.back.domain.auth.entity.UserSocialAuth
import com.back.domain.user.constant.LoginType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserSocialAuthRepository : JpaRepository<UserSocialAuth, Long> {
    fun findByUserUserId(userId: Long): UserSocialAuth?

    // user 연관관계가 LAZY이므로 JOIN FETCH로 즉시 로드 — 세션 종료 후 LazyInitializationException 방지
    @Query("SELECT s FROM UserSocialAuth s JOIN FETCH s.user WHERE s.provider = :provider AND s.providerId = :providerId")
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
