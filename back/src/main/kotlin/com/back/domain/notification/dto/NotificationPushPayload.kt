package com.back.domain.notification.dto

import com.back.domain.notification.entity.Notification

// SSE 전송용 payload. Ut.json의 기본 ObjectMapper는 JavaTimeModule이 없으므로
// LocalDateTime 등 날짜 타입은 포함하지 않는다.
data class NotificationPushPayload(
    val notificationId: Long,
    val type: String,
    val actorId: Long,
    val actorName: String,
    val actorProfileImgUrl: String,
    val targetType: String?,
    val targetId: Long?,
) {
    companion object {
        fun from(notification: Notification): NotificationPushPayload {
            val actor = notification.actor
            return NotificationPushPayload(
                notificationId = notification.notificationId!!,
                type = notification.type.name,
                actorId = actor.userId!!,
                actorName = actor.name,
                actorProfileImgUrl = actor.profileImgUrlOrDefault,
                targetType = notification.targetType,
                targetId = notification.targetId,
            )
        }
    }
}
