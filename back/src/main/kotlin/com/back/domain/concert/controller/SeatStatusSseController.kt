package com.back.domain.concert.controller

import com.back.domain.concert.dto.SeatSnapshotResponse
import com.back.domain.concert.sse.SeatStatusSseEmitterRegistry
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.global.annotation.ApiV1
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@ApiV1
@RestController
@RequestMapping("/concerts")
@Tag(name = "Concert SSE", description = "실시간 좌석 상태 SSE API")
class SeatStatusSseController(
    private val registry: SeatStatusSseEmitterRegistry,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping(
        value = ["/{concertId}/schedules/{scheduleId}/seats/status"],
        produces = ["text/event-stream;charset=UTF-8"]
    )
    @Operation(
        summary = "실시간 좌석 상태 SSE 스트림",
        description = "좌석 상태 변경 이벤트(선점/만료/결제완료)를 실시간으로 수신합니다."
    )
    fun seatStatusStream(
        @PathVariable concertId: Long,
        @PathVariable scheduleId: Long,
        @RequestHeader(value = "Last-Event-ID", required = false) lastEventHeader: String?,
        @RequestParam(value = "lastEventId", required = false) lastEventParam: String?,
        response: HttpServletResponse
    ): ResponseEntity<SseEmitter> {
        response.setHeader("Cache-Control", "no-cache")     // 중간 캐시 서버 캐싱 차단
        response.setHeader("X-Accel-Buffering", "no")       // Nginx proxy_buffering 차단
        response.setHeader("Keep-Alive", "timeout=1800")    // 클라이언트 연결 유지 힌트

        val lastEventId = lastEventHeader?.takeIf { it.isNotBlank() } ?: lastEventParam?.takeIf { it.isNotBlank() }
        val emitter = SseEmitter(SSE_TIMEOUT_MS)

        // register()가 null을 반환하면 연결 수 상한 초과 → 503 Service Unavailable 응답
        val wrapper = registry.register(scheduleId, emitter, lastEventId)
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()

        runCatching {
            val seats = scheduleSeatRepository.findByScheduleScheduleId(scheduleId)
            val snapshotList = seats.map { seat ->
                SeatSnapshotResponse(seat.seatNumber, seat.seatStatus.name)
            }
            val snapshotJson = objectMapper.writeValueAsString(snapshotList)

            val snapshotId = "snapshot:$scheduleId:${System.currentTimeMillis()}"
            wrapper.send(
                SseEmitter.event()
                    .id(snapshotId)
                    .name("seat_snapshot")
                    .data(snapshotJson)
            )
        }.onFailure { e ->
            log.warn("SSE 스냅샷 전송 실패: scheduleId={}", scheduleId, e)
            emitter.complete()
        }

        log.debug("SSE 스트림 연결: concertId={}, scheduleId={}, lastEventId={}", concertId, scheduleId, lastEventId)
        return ResponseEntity.ok(emitter)
    }

    companion object {
        private const val SSE_TIMEOUT_MS = 30 * 60 * 1000L // 30분
    }
}
