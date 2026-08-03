package com.back.domain.waiting.controller

import com.back.domain.waiting.service.WaitingQueueService
import com.back.domain.waiting.sse.QueueSseEmitterRegistry
import com.back.global.annotation.ApiV1
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.requestcontext.RequestContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@ApiV1
@RestController
@RequestMapping("/waiting")
@Tag(name = "Waiting SSE", description = "Waiting SSE API")
class WaitingQueueSseController(
    private val waitingQueueService: WaitingQueueService,
    private val requestContext: RequestContext,
    private val registry: QueueSseEmitterRegistry,
) {
    @GetMapping(
        value = ["/concerts/{concertId}/schedules/{scheduleId}/events"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE],
    )
    @Operation(summary = "대기열 SSE 구독", description = "대기열 순번과 입장 허용 이벤트를 실시간으로 수신합니다.")
    fun subscribe(
        @PathVariable concertId: Long,
        @PathVariable scheduleId: Long,
        response: HttpServletResponse,
    ): SseEmitter {
        val actor = requestContext.actor
            ?: throw ServiceException(ErrorCode.AUTH_LOGIN_REQUIRED)
        waitingQueueService.validateSseSubscription(concertId, scheduleId, actor.id)

        response.setHeader("Cache-Control", "no-cache")
        response.setHeader("X-Accel-Buffering", "no")

        val emitter = SseEmitter(SSE_TIMEOUT_MS)
        val wrapper = registry.register(scheduleId, actor.id, emitter)

        try {
            val connectionState = waitingQueueService.getConnectionState(concertId, scheduleId, actor.id)
            wrapper.send(
                SseEmitter.event()
                    .name(QueueSseEmitterRegistry.CONNECTED_EVENT)
                    .data(connectionState),
            )
        } catch (e: Exception) {
            emitter.completeWithError(e)
        }

        return emitter
    }

    companion object {
        private const val SSE_TIMEOUT_MS = 30 * 60 * 1000L
    }
}
