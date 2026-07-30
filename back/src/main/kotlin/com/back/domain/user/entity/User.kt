package com.back.domain.user.entity

import com.back.domain.user.constant.LoginType

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.jpa.converter.EncryptedStringConverter
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_users_social_account",
            columnNames = ["social_provider", "social_provider_id"],
        ),
    ],
)
class User(
    loginId: String,
    email: String,
    password: String,
    name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val loginType: LoginType,

    oauthRefreshToken: String? = null,
    socialProvider: LoginType? = null,
    socialProviderId: String? = null,
) : BaseEntity() {

    @Column(name = "id", nullable = false, unique = true)
    var loginId: String = loginId
        protected set

    @Column(nullable = false, unique = true)
    var email: String = email
        protected set

    @Column(nullable = false)
    var password: String = password
        protected set

    @Column(nullable = false)
    var name: String = name
        protected set

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter::class)
    var oauthRefreshToken: String? = oauthRefreshToken
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider")
    var socialProvider: LoginType? = socialProvider
        protected set

    @Column(name = "social_provider_id")
    var socialProviderId: String? = socialProviderId
        protected set

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val userId: Long? = null

    var deletedAt: LocalDate? = null
        protected set

    @Column(name = "profile_img_url")
    var profileImgUrl: String? = null
        protected set

    val profileImgUrlOrDefault: String
        get() = profileImgUrl?.let { "$UPLOAD_BASE_URL/$it" } ?: DEFAULT_PROFILE_IMG_URL

    val redirectToProfileImgUrlOrDefault: String
        get() = "$BASE_URL/api/v1/users/$userId/redirectToProfileImg"

    fun updateProfileImg(url: String?) {
        this.profileImgUrl = url
    }

    val isDeleted: Boolean
        get() = deletedAt != null

    fun withdraw() {
        val uuid = UUID.randomUUID().toString()
        this.deletedAt = LocalDate.now()
        this.loginId = uuid
        this.email = "$uuid@deleted.local"
        this.oauthRefreshToken = null
        this.socialProvider = null
        this.socialProviderId = null
        this.profileImgUrl = null
    }

    fun updateOauthRefreshToken(oauthRefreshToken: String?) {
        this.oauthRefreshToken = oauthRefreshToken
    }

    fun linkSocialAccount(
        provider: LoginType,
        providerId: String,
        oauthRefreshToken: String?,
    ) {
        require(provider != LoginType.NORMAL) { "NORMAL은 소셜 제공자가 아닙니다." }
        require(providerId.isNotBlank()) { "소셜 제공자 사용자 ID는 비어 있을 수 없습니다." }
        require(socialProvider == null && socialProviderId == null) {
            "이미 소셜 계정이 연결되어 있습니다."
        }

        this.socialProvider = provider
        this.socialProviderId = providerId
        this.oauthRefreshToken = oauthRefreshToken
    }

    fun unlinkSocialAccount() {
        this.socialProvider = null
        this.socialProviderId = null
        this.oauthRefreshToken = null
    }

    fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.contains(" ")) {
            throw ServiceException(ErrorCode.USER_NAME_INVALID)
        }
        this.name = trimmed
    }

    fun updatePassword(password: String) {
        this.password = password
    }

    companion object {
        // TODO: 배포 도메인 확정되면 application.yaml 설정값으로 옮기기
        private const val BASE_URL = "http://localhost:8080"
        private const val UPLOAD_BASE_URL = "$BASE_URL/uploads"
        private const val DEFAULT_PROFILE_IMG_URL = "$BASE_URL/static/default-profile.png"

        fun create(
            loginId: String, email: String, password: String, name: String, loginType: LoginType
        ): User = User(loginId, email, password, name, loginType)

        fun createOAuth(
            loginId: String, email: String, password: String, name: String,
            loginType: LoginType, providerId: String, oauthRefreshToken: String?
        ): User = User(
            loginId = loginId,
            email = email,
            password = password,
            name = name,
            loginType = loginType,
            oauthRefreshToken = oauthRefreshToken,
            socialProvider = loginType,
            socialProviderId = providerId,
        )
    }
}
