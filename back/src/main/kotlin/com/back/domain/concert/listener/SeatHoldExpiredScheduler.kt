package com.back.domain.concert.listener

import com.back.domain.concert.service.SeatOccupyManager
import org.redisson.client.codec.StringCodec
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 1초 주기로 Redis ZSET(seat:hold:expire:queue)을 폴링하여 만료된 좌석 선점을 자동 해제한다.
 *
 * - Score <= 현재 시각인 항목을 만료된 좌석으로 판단한다.
 * - DB 처리는 SKIP LOCKED를 사용하여 결제/선점 트랜잭션과의 충돌을 방지한다.
 * - SKIP된 좌석은 다음 주기(1초 후)에 재시도된다.
 * - 서버 재부팅 후에도 Redis ZSET이 영속화되어 있어 100% 누락 없이 복구된다.
 */
@Component
class SeatHoldExpiredScheduler(
    private val redissonClient: RedissonClient,
    private val seatHoldExpiredProcessor: SeatHoldExpiredProcessor
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1000)
    fun processExpiredSeats() {
        val now = System.currentTimeMillis().toDouble()
        val expireSet = redissonClient.getScoredSortedSet<String>(SeatOccupyManager.EXPIRE_QUEUE_KEY, StringCodec.INSTANCE)

        // Score(만료 타임스탬프) <= 현재 시각인 항목 전체 조회
        val expiredMembers = expireSet.valueRange(0.0, true, now, true)
        if (expiredMembers.isEmpty()) return

        log.debug("만료 좌석 처리 시작: {}건", expiredMembers.size)

        for (member in expiredMembers) {
            val parts = member.split(":")
            if (parts.size != 3) {
                log.warn("잘못된 만료 큐 멤버 형식, 제거: {}", member)
                expireSet.remove(member)
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
