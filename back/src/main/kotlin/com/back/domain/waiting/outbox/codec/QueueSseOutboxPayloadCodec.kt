package com.back.domain.waiting.outbox.codec

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Component

@Component
class QueueSseOutboxPayloadCodec {
    private val objectMapper = jacksonObjectMapper()

    fun encode(payload: Any): String = objectMapper.writeValueAsString(payload)

    fun <T> decode(payload: String, type: Class<T>): T = objectMapper.readValue(payload, type)
}
