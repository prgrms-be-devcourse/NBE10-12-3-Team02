package com.back.domain.auth.entity

import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.global.jpa.converter.EncryptedStringConverter
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "user_social_auth",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_social_auth_user",
            columnNames = ["user_id"],
        ),
        UniqueConstraint(
            name = "uk_user_social_auth_provider_account",
            columnNames = ["provider", "provider_id"],
        ),
    ],
)
class UserSocialAuth(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: LoginType,

    @Column(name = "provider_id", nullable = false)
    val providerId: String,

    oauthRefreshToken: String?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val socialAuthId: Long? = null

    @Column(name = "oauth_refresh_token", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter::class)
    var oauthRefreshToken: String? = oauthRefreshToken
        protected set

    init {
        require(provider != LoginType.NORMAL) { "NORMAL은 소셜 제공자가 아닙니다." }
        require(providerId.isNotBlank()) { "소셜 제공자 사용자 ID는 비어 있을 수 없습니다." }
    }

    fun updateRefreshToken(refreshToken: String) {
        require(refreshToken.isNotBlank()) { "OAuth Refresh Token은 비어 있을 수 없습니다." }
        oauthRefreshToken = refreshToken
    }
}
