package com.back.domain.notification.dto

import com.back.domain.notification.entity.Notification
import com.back.domain.notification.entity.NotificationType
import java.time.LocalDateTime

data class NotificationResponse(
    val notificationId: Long,
    val type: NotificationType,
    val actorId: Long,
    val actorName: String,
    val actorProfileImgUrl: String,
    val targetType: String?,
    val targetId: Long?,
    val isRead: Boolean,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse {
            val actor = notification.actor
            return NotificationResponse(
                notificationId = notification.notificationId!!,
                type = notification.type,
                actorId = actor.userId!!,
                actorName = actor.name,
                actorProfileImgUrl = actor.profileImgUrlOrDefault,
                targetType = notification.targetType,
                targetId = notification.targetId,
                isRead = notification.isRead,
                createdAt = notification.createDate,
            )
        }
    }
}
