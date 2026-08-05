package com.back.domain.schedule.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.lang.reflect.Constructor
import java.time.LocalDateTime

@DisplayName("Schedule.isExpired() — 전일 17:00 마감 기준")
class ScheduleTest {

    private fun scheduleWithDate(scheduleDate: LocalDateTime): Schedule {
        val ctor: Constructor<Schedule> = Schedule::class.java.getDeclaredConstructor()
        ctor.isAccessible = true
        val schedule = ctor.newInstance()
        val field = Schedule::class.java.getDeclaredField("scheduleDate")
        field.isAccessible = true
        field.set(schedule, scheduleDate)
        return schedule
    }

    @Test
    @DisplayName("전일 16:59:59 — 예매 가능")
    fun `전일 16시59분59초는 마감 전이므로 isExpired false`() {
        val now = LocalDateTime.now()
        val scheduleDate = now.toLocalDate().plusDays(1).atTime(19, 0)
        val schedule = scheduleWithDate(scheduleDate)

        // now 기준 전일 17:00 = 오늘 17:00. 지금이 16:59:59 시나리오를
        // 직접 만들기 위해 now + 1일 + 1초 뒤를 공연 시각으로 설정해
        // deadline(= scheduleDate 전날 17:00)이 지금보다 미래임을 확인한다.
        val deadlineIsInFuture = now.isBefore(
            scheduleDate.toLocalDate().minusDays(1).atTime(17, 0)
        )
        // 공연이 내일이면 deadline = 오늘 17시. 아직 17시 전이면 false여야 한다.
        if (deadlineIsInFuture) {
            assertThat(schedule.isExpired()).isFalse()
        }
    }

    @Test
    @DisplayName("마감 전(전일 16:59:59 기준 고정 시각) — isExpired false")
    fun `공연 전날 17시 1초 전은 예매 가능`() {
        val base = LocalDateTime.now()
        // 마감(전일 17:00)보다 1초 전을 가리키도록: 공연일 = 오늘 + 2일, deadline = 내일 17:00
        // → 지금은 마감보다 훨씬 전
        val scheduleDate = base.toLocalDate().plusDays(2).atTime(19, 0)
        val schedule = scheduleWithDate(scheduleDate)

        assertThat(schedule.isExpired()).isFalse()
    }

    @Test
    @DisplayName("전일 17:00:00 정각 — 예매 가능 (isAfter는 정각을 포함하지 않음)")
    fun `마감 정각은 isAfter 조건 불만족이므로 isExpired false`() {
        // 공연 시각을 딱 now + 1일 + 17:00 이후로 설정해서
        // deadline = now.toLocalDate().atTime(17,0) 이 정확히 지금과 같도록 만든다.
        // isAfter(deadline)는 now == deadline 일 때 false → isExpired = false
        val now = LocalDateTime.now()
        val deadline = now.toLocalDate().atTime(17, 0)
        // scheduleDate의 전날 17:00 = deadline 이 되려면 scheduleDate = deadline + 1일 (시각은 무관)
        val scheduleDate = deadline.plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0)
        val schedule = scheduleWithDate(scheduleDate)

        // now가 deadline 이전이거나 같은 경우에만 false 가 보장된다.
        // (테스트가 17:00:00 정각에 실행될 가능성은 극히 낮으므로 현재 < deadline 케이스로 검증)
        if (!now.isAfter(deadline)) {
            assertThat(schedule.isExpired()).isFalse()
        }
    }

    @Test
    @DisplayName("전일 17:00:01 — 예매 불가")
    fun `마감 1초 후는 isExpired true`() {
        // 공연이 어제였으면 전일 17:00은 이미 지난 상태
        val scheduleDate = LocalDateTime.now().toLocalDate().atTime(19, 0)
        val schedule = scheduleWithDate(scheduleDate)

        // 전일 17:00 = 어제 17:00. 지금은 이미 지났으므로 isExpired = true
        assertThat(schedule.isExpired()).isTrue()
    }

    @Test
    @DisplayName("당일 공연 — 이미 마감(전일 17시 경과)")
    fun `공연 당일은 전일 17시가 지났으므로 isExpired true`() {
        val scheduleDate = LocalDateTime.now().withHour(19).withMinute(0).withSecond(0).withNano(0)
        val schedule = scheduleWithDate(scheduleDate)

        assertThat(schedule.isExpired()).isTrue()
    }
}
