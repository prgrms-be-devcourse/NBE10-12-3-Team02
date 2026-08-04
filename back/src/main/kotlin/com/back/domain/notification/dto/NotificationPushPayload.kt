package com.back.domain.notification.dto

import com.back.domain.notification.entity.Notification
import java.time.LocalDateTime

// SSE 전송용 payload. Ut.json의 ObjectMapper에 JavaTimeModule이 등록되어 있어
// LocalDateTime을 그대로 직렬화해도 ISO-8601 문자열로 나온다 (GET /notifications와 동일 형식).
data class NotificationPushPayload(
    val notificationId: Long,
    val type: String,
    val actorId: Long,
    val actorName: String,
    val actorProfileImgUrl: String,
    val targetType: String?,
    val targetId: Long?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(notification: Notification): NotificationPushPayload {
            val actor = notification.actor
            return NotificationPushPayload(
                notificationId = notification.notificationIdOrThrow,
                type = notification.type.name,
                actorId = actor.userIdOrThrow,
                actorName = actor.name,
                actorProfileImgUrl = actor.profileImgUrlOrDefault,
                targetType = notification.targetType,
                targetId = notification.targetId,
                createdAt = notification.createDate,
            )
        }
    }
}
