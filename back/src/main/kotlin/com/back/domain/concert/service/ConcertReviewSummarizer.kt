package com.back.domain.concert.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component

@Component
class ConcertReviewSummarizer(
    private val chatClient: ChatClient,
) {
    fun summarize(reviews: List<String>): String {
        val userMessage = reviews.mapIndexed { i, content -> "${i + 1}. $content" }.joinToString("\n")

        val raw = chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(userMessage)
            .call()
            .content()

        // LLM이 지침(3~4줄)을 넘겨서 응답해도 안전하도록 코드 레벨에서 한 번 더 자른다.
        return (raw ?: "").trim().take(MAX_SUMMARY_LENGTH)
    }

    companion object {
        private const val MAX_SUMMARY_LENGTH = 500
        private const val SYSTEM_PROMPT =
            "여러 명의 관람후기를 종합해서 공통된 감상을 3~4줄로 요약해줘. 서로 다른 의견이 있으면 균형있게 반영해줘. " +
                "원문에 없는 내용은 절대 추가하지 말고, 원문에 실제로 있는 내용만으로 요약해줘."
    }
}
