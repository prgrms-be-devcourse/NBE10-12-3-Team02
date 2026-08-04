package com.back.domain.notification.dto

import com.back.domain.notification.entity.Notification

// SSE 전송용 payload. Ut.json의 기본 ObjectMapper는 JavaTimeModule이 없어 LocalDateTime을
// 직접 넣으면 직렬화가 실패하므로(전체 payload가 "{}"로 대체됨), createdAt은 toString()으로
// 미리 문자열(ISO-8601)화해서 넣는다. GET /notifications(NotificationResponse.createdAt)가
// 내려주는 형식과 동일해서 프론트에서 같은 알림의 SSE 수신 시각과 재조회 시각이 일치한다.
data class NotificationPushPayload(
    val notificationId: Long,
    val type: String,
    val actorId: Long,
    val actorName: String,
    val actorProfileImgUrl: String,
    val targetType: String?,
    val targetId: Long?,
    val createdAt: String?,
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
                createdAt = notification.createDate?.toString(),
            )
        }
    }
}
