package com.back.domain.user.service

import com.back.domain.auth.service.EmailVerificationService
import com.back.domain.ticket.event.TicketCancelledEvent
import com.back.domain.ticket.repository.TicketRepository
import com.back.domain.user.dto.*
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.file.FileStorage
import com.back.global.security.filter.BearerTokenExtractor
import com.back.global.security.jwt.JwtTokenProvider
import com.back.global.security.jwt.repository.BlacklistRepository
import com.back.global.security.jwt.repository.RefreshTokenRepository
import com.back.global.security.oauth2.service.OAuthUnlinkService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val ticketRepository: TicketRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailVerificationService: EmailVerificationService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val blacklistRepository: BlacklistRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val bearerTokenExtractor: BearerTokenExtractor,
    private val oAuthUnlinkService: OAuthUnlinkService,
    private val eventPublisher: ApplicationEventPublisher,
    private val fileStorage: FileStorage,
    @Value("\${custom.jwt.blacklist.grace-seconds}") private val tokenBlacklistGraceSeconds: Long
) {

    @Transactional
    fun signup(request: SignupRequest): SignupResponse = with(request) {
        val reqId = checkNotNull(id) { "Id is required" }
        val reqEmail = checkNotNull(email) { "Email is required" }
        val reqPassword = checkNotNull(password) { "Password is required" }
        val reqName = checkNotNull(name) { "Name is required" }
        val reqToken = checkNotNull(verificationToken) { "Verification token is required" }

        if (userRepository.existsByLoginIdAndDeletedAtIsNull(reqId)) {
            throw ServiceException(ErrorCode.USER_ID_ALREADY_EXISTS)
        }
        if (userRepository.existsByEmailAndDeletedAtIsNull(reqEmail)) {
            throw ServiceException(ErrorCode.USER_EMAIL_ALREADY_EXISTS)
        }

        val reservationId = emailVerificationService.reserveVerification(reqEmail, reqToken)
            ?: throw ServiceException(ErrorCode.AUTH_EMAIL_VERIFICATION_REQUIRED)

        val encodedPassword = requireNotNull(passwordEncoder.encode(reqPassword)) { "Password encoding failed" }

        val user = userRepository.save(
            User.create(
                loginId = reqId,
                email = reqEmail,
                password = encodedPassword,
                name = reqName,
                loginType = LoginType.NORMAL
            )
        )

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                emailVerificationService.completeVerification(reqEmail, reqToken, reservationId)
            }

            override fun afterCompletion(status: Int) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    emailVerificationService.restoreVerification(reqEmail, reqToken, reservationId)
                }
            }
        })

        return SignupResponse.from(user)
    }

    @Transactional
    fun withdraw(userId: Long, authorization: String) {
        val accessToken = bearerTokenExtractor.extract(authorization)
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND_OR_DELETED)

        user.profileImgUrl?.let { fileStorage.delete(it) }

        if (user.loginType != LoginType.NORMAL) {
            oAuthUnlinkService.unlink(user.loginType, user.oauthRefreshToken)
        }

        val activeTickets = ticketRepository.findAllByUserWithConcert(user).filter { it.isValid }
        for (ticket in activeTickets) {
            ticket.cancel()
            ticket.scheduleSeat.releaseToAvailable()

            val concertId = checkNotNull(ticket.schedule.concert.concertId) { "Concert ID is null" }
            val scheduleId = checkNotNull(ticket.schedule.scheduleId) { "Schedule ID is null" }

            eventPublisher.publishEvent(TicketCancelledEvent(concertId = concertId, scheduleId = scheduleId, userId = userId))
        }

        user.withdraw()
        refreshTokenRepository.deleteAllByUserId(userId)
        val remaining = jwtTokenProvider.getRemainingSeconds(accessToken)
        blacklistRepository.add(accessToken, Duration.ofSeconds(remaining + tokenBlacklistGraceSeconds))
    }

    fun getMyPage(userId: Long): MyPageResponse {
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        val ticketGroups = ticketRepository.findAllByUserWithConcert(user)
            .groupBy { it.groupToken ?: it.ticketId.toString() }
            .values
            .map { TicketGroupInfo.from(it) }

        return MyPageResponse.from(user, ticketGroups)
    }

    @Transactional
    fun updateMyPage(userId: Long, request: UpdateMyPageRequest) {
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        request.name?.let { user.updateName(it) }
        request.email?.let { email ->
            if (user.email != email && userRepository.existsByEmailAndDeletedAtIsNull(email)) {
                throw ServiceException(ErrorCode.USER_EMAIL_ALREADY_EXISTS)
            }
            user.updateEmail(email)
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
}
