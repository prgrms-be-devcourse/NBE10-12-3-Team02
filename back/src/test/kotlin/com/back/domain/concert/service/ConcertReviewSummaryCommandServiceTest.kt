package com.back.domain.concert.service

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.Optional

class ConcertReviewSummaryCommandServiceTest {

    private fun concert(id: Long): Concert =
        Concert.create("콘서트", "설명", LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1), null)
            .also { ReflectionTestUtils.setField(it, "concertId", id) }

    @Test
    @DisplayName("콘서트가 존재하면 종합 요약을 저장한다")
    fun t1() {
        val repository = mock(ConcertRepository::class.java)
        val service = ConcertReviewSummaryCommandService(repository)
        val concert = concert(10L)
        `when`(repository.findById(10L)).thenReturn(Optional.of(concert))

        service.saveSummary(10L, "전반적으로 만족도가 높았다.")

        assertThat(concert.reviewSummary).isEqualTo("전반적으로 만족도가 높았다.")
    }

    @Test
    @DisplayName("콘서트가 존재하지 않으면 아무 것도 하지 않는다")
    fun t2() {
        val repository = mock(ConcertRepository::class.java)
        val service = ConcertReviewSummaryCommandService(repository)
        `when`(repository.findById(999L)).thenReturn(Optional.empty())

        service.saveSummary(999L, "전반적으로 만족도가 높았다.")
        // 예외 없이 조용히 반환되면 성공
    }
}
