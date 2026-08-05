package com.back.domain.concert.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.ai.chat.client.ChatClient

class ConcertReviewSummarizerTest {

    private fun mockChatClient(response: String?): ChatClient {
        val chatClient = mock(ChatClient::class.java)
        val requestSpec = mock(ChatClient.ChatClientRequestSpec::class.java)
        val callResponseSpec = mock(ChatClient.CallResponseSpec::class.java)

        `when`(chatClient.prompt()).thenReturn(requestSpec)
        `when`(requestSpec.system(anyString())).thenReturn(requestSpec)
        `when`(requestSpec.user(anyString())).thenReturn(requestSpec)
        `when`(requestSpec.call()).thenReturn(callResponseSpec)
        `when`(callResponseSpec.content()).thenReturn(response)

        return chatClient
    }

    @Test
    @DisplayName("500자 이내 응답은 그대로 반환한다")
    fun t1() {
        val chatClient = mockChatClient("무대 연출과 음향에 대한 만족도가 높았고, 일부는 좌석 간격이 아쉬웠다는 의견도 있었다.")
        val summarizer = ConcertReviewSummarizer(chatClient)

        val result = summarizer.summarize(listOf("최고였다", "좌석이 좁았다", "연출이 훌륭했다"))

        assertThat(result).isEqualTo("무대 연출과 음향에 대한 만족도가 높았고, 일부는 좌석 간격이 아쉬웠다는 의견도 있었다.")
    }

    @Test
    @DisplayName("500자를 초과하는 응답은 500자로 자른다")
    fun t2() {
        val longResponse = "가".repeat(600)
        val chatClient = mockChatClient(longResponse)
        val summarizer = ConcertReviewSummarizer(chatClient)

        val result = summarizer.summarize(listOf("리뷰1", "리뷰2", "리뷰3"))

        assertThat(result).hasSize(500)
        assertThat(result).isEqualTo(longResponse.take(500))
    }

    @Test
    @DisplayName("앞뒤 공백은 트렁케이션 전에 제거한다")
    fun t3() {
        val chatClient = mockChatClient("  공백이 있는 응답  ")
        val summarizer = ConcertReviewSummarizer(chatClient)

        val result = summarizer.summarize(listOf("리뷰1", "리뷰2", "리뷰3"))

        assertThat(result).isEqualTo("공백이 있는 응답")
    }

    @Test
    @DisplayName("LLM 응답이 null이면 빈 문자열을 반환한다")
    fun t4() {
        val chatClient = mockChatClient(null)
        val summarizer = ConcertReviewSummarizer(chatClient)

        val result = summarizer.summarize(listOf("리뷰1", "리뷰2", "리뷰3"))

        assertThat(result).isEmpty()
    }
}
