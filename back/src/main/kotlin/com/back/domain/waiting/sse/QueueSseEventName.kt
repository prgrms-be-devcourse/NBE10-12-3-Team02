package com.back.domain.waiting.sse

object QueueSseEventName {
    const val CONNECTED = "connected"
    const val QUEUE_STATUS = "queue-status"
    const val ENTRY_ALLOWED = "entry-allowed"
    const val QUEUE_ERROR = "queue-error"
    const val HEARTBEAT = "heartbeat"
}
