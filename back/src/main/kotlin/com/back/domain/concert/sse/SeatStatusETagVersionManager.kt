package com.back.domain.concert.sse

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * 좌석 상태 ETag 버전 카운터 관리 컴포넌트.
 *
 * Redis INCR/GET을 사용하여 스케줄별 ETag 버전을 관리합니다.
 * 좌석 상태 변경 이벤트(선점/취소/만료/결제) 발생 시 increment()를 호출하고,
 * ETag 생성 시에는 getVersion()으로 현재 버전을 조회합니다.
 *
 * 이 방식은 30초 주기 ETag 폴링 요청마다 발생하던 DB 전체 좌석 조회(O(N))를
 * Redis 단일 GET(O(1))으로 대체하여 DB CPU/IOPS 병목을 제거합니다.
 */
@Component
class SeatStatusETagVersionManager(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 현재 ETag 버전을 반환합니다. 키가 없으면 0을 반환합니다.
     */
    fun getVersion(scheduleId: Long): Long {
        return try {
            redisTemplate.opsForValue().get(key(scheduleId))?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            log.warn("ETag 버전 조회 실패 (기본값 0 반환): scheduleId={}, err={}", scheduleId, e.message)
            0L
        }
    }

    /**
     * ETag 버전을 1 증가시킵니다. 좌석 상태가 변경될 때마다 호출합니다.
     */
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
