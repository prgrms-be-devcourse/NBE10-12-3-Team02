package com.back.domain.concert.sse

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

 // Redis INCR/GET을 사용하여 스케줄별 ETag 버전을 관리
 // 좌석 상태 변경 이벤트 발생 시 increment()를 호출하고, ETag 생성 시에는 getVersion()으로 현재 버전을 조회
@Component
class SeatStatusETagVersionManager(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

     // 현재 ETag 버전을 반환, 키가 없으면 0을 반환
    fun getVersion(scheduleId: Long): Long {
        return try {
            redisTemplate.opsForValue().get(key(scheduleId))?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            log.warn("ETag 버전 조회 실패 (기본값 0 반환): scheduleId={}, err={}", scheduleId, e.message)
            0L
        }
    }

     // ETag 버전을 1 증가, 좌석 상태가 변경될 때마다 호출
    fun increment(scheduleId: Long) {
        try {
            redisTemplate.opsForValue().increment(key(scheduleId))
        } catch (e: Exception) {
            log.warn("ETag 버전 증가 실패 (무시됨): scheduleId={}, err={}", scheduleId, e.message)
        }
    }

    companion object {
        fun key(scheduleId: Long) = "schedule:$scheduleId:etag-version"
    }
}
