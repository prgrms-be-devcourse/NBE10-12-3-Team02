package com.back.domain.post.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component

@Component
class ReviewSummarizer(
    private val chatClient: ChatClient,
) {
    fun summarize(reviewContent: String): String {
        val raw = chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(reviewContent)
            .call()
            .content()

        // LLM이 지침(20자 내외)을 넘겨서 응답해도 안전하도록 코드 레벨에서 한 번 더 자른다.
        return (raw ?: "").trim().take(MAX_SUMMARY_LENGTH)
    }

    companion object {
        private const val MAX_SUMMARY_LENGTH = 30
        private const val SYSTEM_PROMPT =
            "리뷰를 한 문장, 20자 내외로 요약해줘. 핵심 감상(좋았던 점/아쉬운 점) 위주로 작성해줘. " +
                "원문에 없는 내용은 절대 추가하지 말고, 원문에 실제로 있는 내용만으로 요약해줘. " +
                "원문이 짧으면 요약도 그만큼 짧게 유지해줘."
    }
}
