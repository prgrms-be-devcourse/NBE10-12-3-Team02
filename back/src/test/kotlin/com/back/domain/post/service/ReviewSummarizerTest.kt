package com.back.domain.post.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.ai.chat.client.ChatClient

class ReviewSummarizerTest {

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
    @DisplayName("30자 이내 응답은 그대로 반환한다")
    fun t1() {
        val chatClient = mockChatClient("좋은 공연이었지만 음향이 아쉬웠다")
        val summarizer = ReviewSummarizer(chatClient)

        val result = summarizer.summarize("리뷰 내용")

        assertThat(result).isEqualTo("좋은 공연이었지만 음향이 아쉬웠다")
    }

    @Test
    @DisplayName("30자를 초과하는 응답은 30자로 자른다")
    fun t2() {
        val longResponse = "이 공연은 정말 훌륭했고 무대 연출도 완벽했으며 앙코르까지 감동적이어서 다음에도 꼭 다시 보고 싶은 최고의 공연이었습니다"
        val chatClient = mockChatClient(longResponse)
        val summarizer = ReviewSummarizer(chatClient)

        val result = summarizer.summarize("리뷰 내용")

        assertThat(result).hasSize(30)
        assertThat(result).isEqualTo(longResponse.take(30))
    }

    @Test
    @DisplayName("앞뒤 공백은 트렁케이션 전에 제거한다")
    fun t3() {
        val chatClient = mockChatClient("  공백이 있는 응답  ")
        val summarizer = ReviewSummarizer(chatClient)

        val result = summarizer.summarize("리뷰 내용")

        assertThat(result).isEqualTo("공백이 있는 응답")
    }

    @Test
    @DisplayName("LLM 응답이 null이면 빈 문자열을 반환한다")
    fun t4() {
        val chatClient = mockChatClient(null)
        val summarizer = ReviewSummarizer(chatClient)

        val result = summarizer.summarize("리뷰 내용")

        assertThat(result).isEmpty()
    }
}
