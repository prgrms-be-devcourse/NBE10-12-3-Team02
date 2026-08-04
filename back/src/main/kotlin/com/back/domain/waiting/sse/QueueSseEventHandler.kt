package com.back.domain.waiting.sse

import com.back.domain.queue.event.EntryAllowedEvent
import com.back.domain.queue.event.QueueErrorEvent
import com.back.domain.queue.event.QueueStatusEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
@Async
class QueueSseEventHandler(
    private val registry: QueueSseEmitterRegistry,
) {
    @EventListener
    fun handleQueueStatusUpdated(event: QueueStatusEvent) {
        registry.broadcastStatus(event.scheduleId, event)
    }

    @EventListener
    fun handleEntryAllowed(event: EntryAllowedEvent) {
        registry.sendEntryAllowed(event)
    }

    @EventListener
    fun handleQueueError(event: QueueErrorEvent) {
        registry.sendError(event)
    }
}
