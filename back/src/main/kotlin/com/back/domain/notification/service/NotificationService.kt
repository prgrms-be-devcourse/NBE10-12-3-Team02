package com.back.domain.notification.service

import com.back.domain.notification.dto.NotificationPushPayload
import com.back.domain.notification.dto.NotificationResponse
import com.back.domain.notification.entity.Notification
import com.back.domain.notification.event.PostLikedEvent
import com.back.domain.notification.event.UserFollowedEvent
import com.back.domain.notification.repository.NotificationRepository
import com.back.domain.notification.sse.NotificationSseEmitterRegistry
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val notificationSseEmitterRegistry: NotificationSseEmitterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // AFTER_COMMIT 시점엔 원본 트랜잭션이 이미 끝나 있어 REQUIRES_NEW/NOT_SUPPORTED 외의 propagation은
    // Spring의 RestrictedTransactionalEventListenerFactory(spring-tx, @since 6.1)가 컨텍스트 로딩 시점에 거부한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onPostLiked(event: PostLikedEvent) {
        val receiver = userRepository.findByUserIdAndDeletedAtIsNull(event.postOwnerId) ?: return
        val actor = userRepository.findByUserIdAndDeletedAtIsNull(event.actorId) ?: return

        val notification = notificationRepository.save(Notification.ofLike(receiver, actor, event.postId))
        log.debug("좋아요 알림 생성: receiverId={}, actorId={}, postId={}", event.postOwnerId, event.actorId, event.postId)

        notificationSseEmitterRegistry.send(receiver.userId!!, NotificationPushPayload.from(notification))
    }

    // AFTER_COMMIT 시점엔 원본 트랜잭션이 이미 끝나 있어 REQUIRES_NEW/NOT_SUPPORTED 외의 propagation은
    // Spring의 RestrictedTransactionalEventListenerFactory(spring-tx, @since 6.1)가 컨텍스트 로딩 시점에 거부한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onUserFollowed(event: UserFollowedEvent) {
        val receiver = userRepository.findByUserIdAndDeletedAtIsNull(event.followeeId) ?: return
        val actor = userRepository.findByUserIdAndDeletedAtIsNull(event.followerId) ?: return

        val notification = notificationRepository.save(Notification.ofFollow(receiver, actor))
        log.debug("팔로우 알림 생성: receiverId={}, actorId={}", event.followeeId, event.followerId)

        notificationSseEmitterRegistry.send(receiver.userId!!, NotificationPushPayload.from(notification))
    }

    @Transactional(readOnly = true)
    fun getMyNotifications(userId: Long, pageable: Pageable): Page<NotificationResponse> =
        notificationRepository.findPageByReceiverId(userId, pageable)
            .map(NotificationResponse::from)

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: Long): Long =
        notificationRepository.countByReceiverUserIdAndIsReadFalse(userId)

    @Transactional
    fun markAsRead(notificationId: Long, userId: Long) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ServiceException(ErrorCode.NOTIFICATION_NOT_FOUND) }

        if (notification.receiver.userId != userId) {
            throw ServiceException(ErrorCode.NOTIFICATION_FORBIDDEN)
        }

        notification.markAsRead()
    }

    @Transactional
    fun markAllAsRead(userId: Long) {
        notificationRepository.markAllAsRead(userId)
    }
}
