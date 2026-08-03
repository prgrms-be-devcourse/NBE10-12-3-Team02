package com.back.domain.notification.controller

import com.back.domain.notification.dto.NotificationResponse
import com.back.domain.notification.service.NotificationService
import com.back.domain.notification.sse.NotificationSseEmitterRegistry
import com.back.global.annotation.ApiV1
import com.back.global.requestcontext.RequestContext
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@ApiV1
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification", description = "알림 API")
class NotificationController(
    private val notificationService: NotificationService,
    private val notificationSseEmitterRegistry: NotificationSseEmitterRegistry,
    private val requestContext: RequestContext,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping(value = ["/subscribe"], produces = ["text/event-stream;charset=UTF-8"])
    @Operation(summary = "알림 실시간 구독", description = "좋아요/팔로우 알림을 SSE로 실시간 수신합니다.")
    fun subscribe(): SseEmitter {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val emitter = SseEmitter(SSE_TIMEOUT_MS)
        val wrapper = notificationSseEmitterRegistry.register(actor.id, emitter)

        try {
            wrapper.send(SseEmitter.event().name("connect").data("connected"))
        } catch (e: Exception) {
            log.warn("SSE 최초 연결 이벤트 전송 실패: userId={}", actor.id, e)
            emitter.complete()
        }

        return emitter
    }

    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "내가 받은 알림을 최신순으로 페이징 조회합니다.")
    fun getMyNotifications(
        @PageableDefault(size = 20) pageable: Pageable,
    ): RsData<Page<NotificationResponse>> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = notificationService.getMyNotifications(actor.id, pageable)
        return RsData("200-1", "알림 목록 조회 성공", data)
    }

    @GetMapping("/unread-count")
    @Operation(summary = "안읽은 알림 개수 조회", description = "읽지 않은 알림 개수를 조회합니다.")
    fun getUnreadCount(): RsData<Long> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        val data = notificationService.getUnreadCount(actor.id)
        return RsData("200-1", "안읽은 알림 개수 조회 성공", data)
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    fun markAsRead(@PathVariable notificationId: Long): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        notificationService.markAsRead(notificationId, actor.id)
        return RsData("200-1", "알림을 읽음 처리했습니다.")
    }

    @PatchMapping("/read-all")
    @Operation(summary = "전체 알림 읽음 처리", description = "내 알림을 모두 읽음 처리합니다.")
    fun markAllAsRead(): RsData<Void> {
        val actor = requestContext.actor ?: throw IllegalStateException("Actor must not be null")
        notificationService.markAllAsRead(actor.id)
        return RsData("200-1", "모든 알림을 읽음 처리했습니다.")
    }

    companion object {
        private const val SSE_TIMEOUT_MS = 30 * 60 * 1000L // 30분
    }
}
