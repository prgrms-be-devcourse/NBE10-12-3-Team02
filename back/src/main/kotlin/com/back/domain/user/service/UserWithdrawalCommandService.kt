package com.back.domain.user.service

import com.back.domain.auth.repository.UserSocialAuthRepository
import com.back.domain.ticket.event.TicketCancelledEvent
import com.back.domain.ticket.repository.TicketRepository
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.file.FileStorage
import com.back.global.security.jwt.JwtTokenProvider
import com.back.global.security.jwt.repository.BlacklistRepository
import com.back.global.security.jwt.repository.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class UserWithdrawalCommandService(
    private val userRepository: UserRepository,
    private val userSocialAuthRepository: UserSocialAuthRepository,
    private val ticketRepository: TicketRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val blacklistRepository: BlacklistRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val eventPublisher: ApplicationEventPublisher,
    private val fileStorage: FileStorage,
    @Value("\${custom.jwt.blacklist.grace-seconds}")
    private val tokenBlacklistGraceSeconds: Long,
) {
    @Transactional
    fun withdraw(userId: Long, accessToken: String) {
        val user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND_OR_DELETED)

        user.profileImgUrl?.let(fileStorage::delete)

        ticketRepository.findAllByUserWithConcert(user)
            .filter { it.isValid }
            .forEach { ticket ->
                ticket.cancel()
                ticket.scheduleSeat.releaseToAvailable()

                eventPublisher.publishEvent(
                    TicketCancelledEvent(
                        concertId = checkNotNull(ticket.schedule.concert.concertId) {
                            "Concert ID is null"
                        },
                        scheduleId = checkNotNull(ticket.schedule.scheduleId) {
                            "Schedule ID is null"
                        },
                        userId = userId,
                    ),
                )
            }

        userSocialAuthRepository.deleteByUserUserId(userId)
        user.withdraw()
        refreshTokenRepository.deleteAllByUserId(userId)

        val remaining = jwtTokenProvider.getRemainingSeconds(accessToken)
        blacklistRepository.add(
            accessToken,
            Duration.ofSeconds(remaining + tokenBlacklistGraceSeconds),
        )
    }
}
