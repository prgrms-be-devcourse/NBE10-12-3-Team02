package com.back.global.initData.fixture

import com.back.domain.concert.entity.Concert
import com.back.domain.schedule.entity.Schedule
import com.back.domain.schedule.repository.ScheduleRepository
import com.back.domain.venue.entity.Venue
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class ScheduleFixture(
    private val scheduleRepository: ScheduleRepository
) {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun dt(value: String): LocalDateTime = LocalDateTime.parse(value, fmt)

    fun createSchedules(concerts: List<Concert>, venues: List<Venue>): List<Schedule> {
        val schedules = buildList {
            // [마감 공연 1] concerts[0]: 개미와 베짱이 [인천] (모든 회차가 과거로 전체 마감)
            add(Schedule.create(concerts[0], venues[10], dt("2026-05-20 14:00:00"), 1))
            add(Schedule.create(concerts[0], venues[9], dt("2026-07-01 16:00:00"), 2))

            // [마감 공연 2] concerts[1]: 제31회 젊은연극제, 한씨연대기 [청주] (모든 회차가 과거로 전체 마감)
            add(Schedule.create(concerts[1], venues[4], dt("2026-06-30 19:30:00"), 1))

            // [마감 공연 3] concerts[2]: 콘크리트 보이스 1: 천변우로 415 (모든 회차가 과거로 전체 마감)
            add(Schedule.create(concerts[2], venues[7], dt("2026-07-05 17:00:00"), 1))

            // [마감 공연 4] concerts[3]: 잠자는 숲속의 공주 [대구] (모든 회차가 과거로 전체 마감)
            add(Schedule.create(concerts[3], venues[6], dt("2026-07-11 14:00:00"), 1))
            add(Schedule.create(concerts[3], venues[6], dt("2026-07-26 16:00:00"), 2))

            // [마감 공연 5] concerts[4]: 백설공주 [서울 노원] (모든 회차가 과거로 전체 마감)
            add(Schedule.create(concerts[4], venues[22], dt("2026-07-18 11:00:00"), 1))
            add(Schedule.create(concerts[4], venues[22], dt("2026-07-31 15:00:00"), 2))

            // [예매 가능 공연들] concerts[5] ~ concerts[28] (미래 일정으로 예매 가능)
            // concerts[5]: 오잉클!
            add(Schedule.create(concerts[5], venues[14], dt("2026-10-20 19:30:00"), 1))
            add(Schedule.create(concerts[5], venues[14], dt("2026-11-20 19:30:00"), 2))

            // concerts[6]: Love & Jazz (발렌타인&화이트 데이) [사당]
            add(Schedule.create(concerts[6], venues[13], dt("2026-10-01 19:30:00"), 1))
            add(Schedule.create(concerts[6], venues[13], dt("2026-11-15 17:00:00"), 2))

            // concerts[7]: 해리엇 드레스 리허설
            add(Schedule.create(concerts[7], venues[15], dt("2026-10-10 15:00:00"), 1))
            add(Schedule.create(concerts[7], venues[15], dt("2026-11-25 19:30:00"), 2))

            // concerts[8]: 드림시어터 (DREAM THEATER)
            add(Schedule.create(concerts[8], venues[16], dt("2026-10-05 19:30:00"), 1))
            add(Schedule.create(concerts[8], venues[16], dt("2026-10-11 17:00:00"), 2))

            // concerts[9]: 파이팅 콘서트 시즌Ⅳ, 무직회사
            add(Schedule.create(concerts[9], venues[3], dt("2026-09-25 19:30:00"), 1))
            add(Schedule.create(concerts[9], venues[3], dt("2026-11-20 19:30:00"), 2))

            // concerts[10]: 카디션 김슬기의 다윈의 역작 (7월)
            add(Schedule.create(concerts[10], venues[11], dt("2026-09-25 19:30:00"), 1))
            add(Schedule.create(concerts[10], venues[11], dt("2026-10-30 19:30:00"), 2))

            // concerts[11]: 부산시립국악관현악단, 다함께 행복한 음악회: 얼씨구
            add(Schedule.create(concerts[11], venues[0], dt("2026-09-30 15:00:00"), 1))
            add(Schedule.create(concerts[11], venues[1], dt("2026-10-22 19:30:00"), 2))

            // concerts[12]: 먼데이프로젝트 시즌9, OUR SUMMER: 심재현 X 히미츠
            add(Schedule.create(concerts[12], venues[20], dt("2026-09-30 20:00:00"), 1))
            add(Schedule.create(concerts[12], venues[19], dt("2026-10-23 20:00:00"), 2))

            // concerts[13]: Academic Guitar Festival, 황민웅 독주회
            add(Schedule.create(concerts[13], venues[5], dt("2026-10-02 17:00:00"), 1))
            add(Schedule.create(concerts[13], venues[5], dt("2026-12-09 19:30:00"), 2))

            // concerts[14]: 난타 [명동]
            add(Schedule.create(concerts[14], venues[1], dt("2026-10-09 17:00:00"), 1))
            add(Schedule.create(concerts[14], venues[2], dt("2026-10-20 20:00:00"), 2))

            // concerts[15]: 쉬어매드니스 [대학로]
            add(Schedule.create(concerts[15], venues[21], dt("2026-10-10 15:00:00"), 1))
            add(Schedule.create(concerts[15], venues[4], dt("2026-10-25 19:00:00"), 2))
            add(Schedule.create(concerts[15], venues[1], dt("2026-11-07 20:00:00"), 3))

            // concerts[16]: 제44회 대한민국 연극제 부산, 모스크바의 바다
            add(Schedule.create(concerts[16], venues[23], dt("2026-10-11 17:00:00"), 1))
            add(Schedule.create(concerts[16], venues[23], dt("2026-10-29 19:30:00"), 2))

            // concerts[17]: 장애인문화예술축제 A+ Festival
            add(Schedule.create(concerts[17], venues[12], dt("2026-10-15 19:00:00"), 1))
            add(Schedule.create(concerts[17], venues[12], dt("2026-10-28 19:30:00"), 2))

            // concerts[18]: 금정클래식위크 2nd. 이음: 금빛누리홀 (패키지)
            add(Schedule.create(concerts[18], venues[18], dt("2026-10-15 15:00:00"), 1))
            add(Schedule.create(concerts[18], venues[17], dt("2026-11-30 19:30:00"), 2))

            // concerts[19]: 나상현씨밴드 클럽투어: 여름빛 [전주]
            add(Schedule.create(concerts[19], venues[20], dt("2026-10-15 18:00:00"), 1))
            add(Schedule.create(concerts[19], venues[20], dt("2026-12-02 19:30:00"), 2))

            // concerts[20]: 월드아트 서커스 [당진]
            add(Schedule.create(concerts[20], venues[11], dt("2026-10-16 14:00:00"), 1))
            add(Schedule.create(concerts[20], venues[9], dt("2027-01-30 15:00:00"), 2))
            add(Schedule.create(concerts[20], venues[8], dt("2027-05-24 17:00:00"), 3))

            // concerts[21]: 춤과 인문학: Part.1, 춤; 움직임, 숨
            add(Schedule.create(concerts[21], venues[2], dt("2026-10-20 15:00:00"), 1))
            add(Schedule.create(concerts[21], venues[8], dt("2027-01-22 19:30:00"), 2))
            add(Schedule.create(concerts[21], venues[10], dt("2027-03-11 19:30:00"), 3))

            // concerts[22]: 제24회 김천 국제 가족 연극제, 빨래터에 봄이 오면
            add(Schedule.create(concerts[22], venues[24], dt("2026-10-20 15:00:00"), 1))
            add(Schedule.create(concerts[22], venues[24], dt("2026-11-05 16:00:00"), 2))

            // concerts[23]: 씨야 20주년 전국 투어 콘서트: THE FAN [대구]
            add(Schedule.create(concerts[23], venues[24], dt("2026-10-22 19:00:00"), 1))
            add(Schedule.create(concerts[23], venues[28], dt("2026-11-25 19:30:00"), 2))
            add(Schedule.create(concerts[23], venues[26], dt("2026-12-22 19:30:00"), 3))

            // concerts[24]: 위클리 클래식 페스티벌, 카메라타 필 앙상블: America 미국 [청주]
            add(Schedule.create(concerts[24], venues[18], dt("2026-10-23 19:30:00"), 1))

            // concerts[25]: 먼데이프로젝트 IN 라이브클럽, 물결 단독콘서트
            add(Schedule.create(concerts[25], venues[25], dt("2026-10-26 20:00:00"), 1))

            // concerts[26]: 나상현씨밴드 클럽투어: 여름빛 [춘천]
            add(Schedule.create(concerts[26], venues[26], dt("2026-10-30 19:00:00"), 1))
            add(Schedule.create(concerts[26], venues[27], dt("2026-12-08 19:30:00"), 2))

            // concerts[27]: 제24회 김천 국제 가족 연극제, 봄날은 간다
            add(Schedule.create(concerts[27], venues[24], dt("2026-10-27 15:00:00"), 1))
            add(Schedule.create(concerts[27], venues[24], dt("2026-11-10 16:00:00"), 2))

            // concerts[28]: nib.archive 전국 클럽투어: idea of nib [광주]
            add(Schedule.create(concerts[28], venues[21], dt("2026-11-12 19:30:00"), 1))
        }

        // 회차 일시(scheduleDate) 기준 오름차순으로 정렬하여 DB에 저장
        return scheduleRepository.saveAll(schedules.sortedBy { it.scheduleDate })
    }
}
