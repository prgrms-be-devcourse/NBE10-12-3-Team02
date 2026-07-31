package com.back.domain.concert.listener

import com.back.domain.concert.service.SeatOccupyManager
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// 1초 주기로 Redis ZSET(seat:hold:expire:queue)을 폴링하여 만료된 좌석 선점을 자동 해제함
// Score <= 현재 시각인 항목을 만료된 좌석으로 판단함
// DB 처리는 SKIP LOCKED를 사용하여 결제/선점 트랜잭션과의 충돌을 방지함
// SKIP된 좌석은 다음 주기에 재시도
// 서버 재부팅 후에도 Redis ZSET이 영속화되어 있어 100% 누락 없이 복구
@Component
class SeatHoldExpiredScheduler(
    private val stringRedisTemplate: StringRedisTemplate,
    private val seatHoldExpiredProcessor: SeatHoldExpiredProcessor
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1000)
    fun processExpiredSeats() {
        val now = System.currentTimeMillis().toDouble()

        // Score <= 현재 시각인 항목 전체 조회
        val expiredMembers = stringRedisTemplate.opsForZSet().rangeByScore(SeatOccupyManager.EXPIRE_QUEUE_KEY, 0.0, now)
        if (expiredMembers.isNullOrEmpty()) return

        log.debug("만료 좌석 처리 시작: {}건", expiredMembers.size)

        for (member in expiredMembers) {
            val parts = member.split(":")
            if (parts.size != 3) {
                log.warn("잘못된 만료 큐 멤버 형식, 제거: {}", member)
                stringRedisTemplate.opsForZSet().remove(SeatOccupyManager.EXPIRE_QUEUE_KEY, member)
                continue
            }

            try {
                val concertId = parts[0].toLong()
                val scheduleId = parts[1].toLong()
                val seatNumber = parts[2]

                seatHoldExpiredProcessor.processExpiredSeat(concertId, scheduleId, seatNumber)
            } catch (e: Exception) {
                log.error("좌석 만료 처리 실패 (다음 주기 재시도): {}, error={}", member, e.message)
            }
        }
    }
}
