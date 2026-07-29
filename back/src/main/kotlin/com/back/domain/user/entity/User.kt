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
@Table(name = "users")
class User(
    loginId: String,
    email: String,
    password: String,
    name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val loginType: LoginType,

    oauthRefreshToken: String? = null
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
        this.profileImgUrl = null
    }

    fun updateOauthRefreshToken(oauthRefreshToken: String?) {
        this.oauthRefreshToken = oauthRefreshToken
    }

    fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.contains(" ")) {
            throw ServiceException(ErrorCode.USER_NAME_INVALID)
        }
        this.name = trimmed
    }

    fun updateEmail(email: String) {
        this.email = email
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
        ): User = User(loginId, email, password, name, loginType, null)

        fun createOAuth(
            loginId: String, email: String, password: String, name: String,
            loginType: LoginType, oauthRefreshToken: String?
        ): User = User(loginId, email, password, name, loginType, oauthRefreshToken)
    }
}