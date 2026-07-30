package com.back.domain.user.service

import com.back.domain.auth.service.EmailVerificationService
import com.back.domain.auth.repository.UserSocialAuthRepository
import com.back.domain.ticket.repository.TicketRepository
import com.back.domain.user.dto.*
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.file.FileStorage
import com.back.global.security.filter.BearerTokenExtractor
import com.back.global.security.oauth2.service.OAuthUnlinkService
import com.back.global.security.oauth2.service.OAuthUnlinkResult
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val userSocialAuthRepository: UserSocialAuthRepository,
    private val ticketRepository: TicketRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailVerificationService: EmailVerificationService,
    private val bearerTokenExtractor: BearerTokenExtractor,
    private val oAuthUnlinkService: OAuthUnlinkService,
    private val userWithdrawalQueryService: UserWithdrawalQueryService,
    private val userWithdrawalCommandService: UserWithdrawalCommandService,
    private val fileStorage: FileStorage,
) {

    @Transactional
    fun signup(request: SignupRequest): SignupResponse {
        val id = request.id ?: throw ServiceException(ErrorCode.BAD_REQUEST)
        val email = request.email ?: throw ServiceException(ErrorCode.BAD_REQUEST)
        val password = request.password ?: throw ServiceException(ErrorCode.BAD_REQUEST)
        val name = request.name ?: throw ServiceException(ErrorCode.BAD_REQUEST)
        val verificationToken = request.verificationToken ?: throw ServiceException(ErrorCode.BAD_REQUEST)

        if (userRepository.existsByLoginIdAndDeletedAtIsNull(id)) {
            throw ServiceException(ErrorCode.USER_ID_ALREADY_EXISTS)
        }
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw ServiceException(ErrorCode.USER_EMAIL_ALREADY_EXISTS)
        }

        val reservationId = emailVerificationService.reserveVerification(email, verificationToken)
            ?: throw ServiceException(ErrorCode.AUTH_EMAIL_VERIFICATION_REQUIRED)

        val encodedPassword = requireNotNull(passwordEncoder.encode(password)) { "Password encoding failed" }

        val user = userRepository.save(
            User.create(
                loginId = id,
                email = email,
                password = encodedPassword,
                name = name,
                loginType = LoginType.NORMAL
            )
        )

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                emailVerificationService.completeVerification(email, verificationToken, reservationId)
            }

            override fun afterCompletion(status: Int) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    emailVerificationService.restoreVerification(email, verificationToken, reservationId)
                }
            }
        })

        return SignupResponse.from(user)
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun withdraw(userId: Long, authorization: String) {
        val accessToken = bearerTokenExtractor.extract(authorization)
        val target = userWithdrawalQueryService.getWithdrawalTarget(userId)

        target.provider?.let { provider ->
            if (
                oAuthUnlinkService.unlink(provider, target.oauthRefreshToken) ==
                OAuthUnlinkResult.FAILED
            ) {
                log.warn(
                    "회원 탈퇴 중 OAuth Provider 연결 해제 실패, 내부 탈퇴 계속: userId={}, provider={}, providerId={}",
                    userId,
                    provider,
                    target.providerId,
                )
            }
        }

        userWithdrawalCommandService.withdraw(userId, accessToken)
    }

    fun getMyPage(userId: Long): MyPageResponse {
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        val ticketGroups = ticketRepository.findAllByUserWithConcert(user)
            .groupBy { it.groupToken ?: it.ticketId.toString() }
            .values
            .map { TicketGroupInfo.from(it) }

        val socialProvider = userSocialAuthRepository.findByUserUserId(userId)?.provider
        return MyPageResponse.from(user, socialProvider, ticketGroups)
    }

    @Transactional
    fun updateMyPage(userId: Long, request: UpdateMyPageRequest) {
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        request.name?.let { user.updateName(it) }
        request.email?.let { email ->
            if (!user.email.equals(email.trim(), ignoreCase = true)) {
                throw ServiceException(ErrorCode.USER_EMAIL_CHANGE_NOT_ALLOWED)
            }
        }
        request.password?.let { password ->
            val encoded = requireNotNull(passwordEncoder.encode(password)) { "Password encoding failed" }
            user.updatePassword(encoded)
        }
    }

    fun checkId(id: String) {
        if (userRepository.existsByLoginIdAndDeletedAtIsNull(id)) {
            throw ServiceException(ErrorCode.USER_ID_ALREADY_EXISTS)
        }
    }

    @Transactional
    fun updateProfileImg(userId: Long, file: MultipartFile): String {
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        val oldPath = user.profileImgUrl

        val storedPath = fileStorage.store(file, "profile")
        user.updateProfileImg(storedPath)

        oldPath?.let { fileStorage.delete(it) }

        return user.redirectToProfileImgUrlOrDefault
    }

    @Transactional
    fun deleteProfileImg(userId: Long) {
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        user.profileImgUrl?.let { fileStorage.delete(it) }
        user.updateProfileImg(null)
    }

    fun getProfileImgRedirectUrl(userId: Long): String {
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        return user.profileImgUrlOrDefault
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserService::class.java)
    }
}
