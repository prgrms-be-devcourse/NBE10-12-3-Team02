package com.back.domain.notification.service

import com.back.domain.notification.entity.Notification
import com.back.domain.notification.repository.NotificationRepository
import com.back.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationCommandService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
) {
    // User 조회와 Notification 저장을 하나의 REQUIRES_NEW 트랜잭션 안에서 처리한다.
    // NotificationService에서 조회한 User 엔티티를 트랜잭션 경계 너머로 넘기면 준영속 상태가 되므로,
    // 이벤트의 ID만 받아 이 트랜잭션 안에서 직접 조회한다.
    // AFTER_COMMIT 시점엔 원본 트랜잭션이 이미 끝나 있어 REQUIRES_NEW/NOT_SUPPORTED 외의 propagation은
    // Spring의 RestrictedTransactionalEventListenerFactory(spring-tx, @since 6.1)가 컨텍스트 로딩 시점에 거부한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveLikeNotification(receiverId: Long, actorId: Long, postId: Long): Notification? {
        val receiver = userRepository.findByUserIdAndDeletedAtIsNull(receiverId) ?: return null
        val actor = userRepository.findByUserIdAndDeletedAtIsNull(actorId) ?: return null
        return notificationRepository.save(Notification.ofLike(receiver, actor, postId))
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveFollowNotification(receiverId: Long, actorId: Long): Notification? {
        val receiver = userRepository.findByUserIdAndDeletedAtIsNull(receiverId) ?: return null
        val actor = userRepository.findByUserIdAndDeletedAtIsNull(actorId) ?: return null
        return notificationRepository.save(Notification.ofFollow(receiver, actor))
    }
}
