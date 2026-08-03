package com.back.domain.notification.service

import com.back.domain.notification.dto.NotificationPushPayload
import com.back.domain.notification.dto.NotificationResponse
import com.back.domain.notification.event.PostLikedEvent
import com.back.domain.notification.event.UserFollowedEvent
import com.back.domain.notification.repository.NotificationRepository
import com.back.domain.notification.sse.NotificationSseEmitterRegistry
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationCommandService: NotificationCommandService,
    private val notificationSseEmitterRegistry: NotificationSseEmitterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // User 조회 + Notification 저장을 NotificationCommandService의 REQUIRES_NEW 트랜잭션으로
    // 완전히 커밋(커넥션 반납)한 뒤에 SSE를 전송한다. 같은 트랜잭션 안에서 send()까지 하면
    // SSE 전송 동안 DB 커넥션을 붙잡고, 커밋 전에 알림이 화면에 뜨는 데이터 불일치가 생길 수
    // 있어 분리했다. receiver/actor가 탈퇴/삭제된 경우 saveXxx가 null을 반환하며 조용히 스킵한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onPostLiked(event: PostLikedEvent) {
        val notification = notificationCommandService
            .saveLikeNotification(event.postOwnerId, event.actorId, event.postId) ?: return
        log.debug("좋아요 알림 생성: receiverId={}, actorId={}, postId={}", event.postOwnerId, event.actorId, event.postId)

        notificationSseEmitterRegistry.send(notification.receiver.userIdOrThrow, NotificationPushPayload.from(notification))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onUserFollowed(event: UserFollowedEvent) {
        val notification = notificationCommandService
            .saveFollowNotification(event.followeeId, event.followerId) ?: return
        log.debug("팔로우 알림 생성: receiverId={}, actorId={}", event.followeeId, event.followerId)

        notificationSseEmitterRegistry.send(notification.receiver.userIdOrThrow, NotificationPushPayload.from(notification))
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
