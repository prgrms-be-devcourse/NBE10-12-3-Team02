package com.back.domain.notification.service

import com.back.domain.notification.entity.Notification
import com.back.domain.notification.repository.NotificationRepository
import com.back.domain.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationCommandService(
    private val notificationRepository: NotificationRepository,
) {
    // AFTER_COMMIT 시점엔 원본 트랜잭션이 이미 끝나 있어 REQUIRES_NEW/NOT_SUPPORTED 외의 propagation은
    // Spring의 RestrictedTransactionalEventListenerFactory(spring-tx, @since 6.1)가 컨텍스트 로딩 시점에 거부한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveLikeNotification(receiver: User, actor: User, postId: Long): Notification =
        notificationRepository.save(Notification.ofLike(receiver, actor, postId))

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveFollowNotification(receiver: User, actor: User): Notification =
        notificationRepository.save(Notification.ofFollow(receiver, actor))
}
