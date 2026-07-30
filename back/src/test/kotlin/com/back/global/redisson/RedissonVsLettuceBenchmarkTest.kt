package com.back.global.redisson

import com.back.global.RedisTestConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

@SpringBootTest(properties = ["spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false", "logging.level.org.hibernate.SQL=OFF"])
@Import(RedisTestConfig::class)
class RedissonVsLettuceBenchmarkTest {

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @Test
    @DisplayName("Redisson Pub/Sub 분산 락 vs StringRedisTemplate 스핀락 100건 동시 경합 벤치마크")
    fun compareRedissonAndSpinLock() {
        val taskCount = 100     // 평상시 빠른 테스트를 위해 100번으로 하향 조정
        val spinLockKey = "benchmark:seat:spin_lock"
        val redissonLockKey = "benchmark:seat:redisson_lock"

        println("==================================================")
        println("Redisson Pub/Sub 락 vs StringRedisTemplate 스핀락 벤치마크 테스트")
        println("동시 경합 요청 수: $taskCount 건")
        println("==================================================")

        // 1. StringRedisTemplate (Lettuce SETNX Spin-Lock + UUID 토큰 소유권 검증)
        val spinRetryCount = AtomicLong(0)
        val spinLatch = CountDownLatch(taskCount)
        val spinTime = Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            measureTimeMillis {
                repeat(taskCount) {
                    executor.submit {
                        val threadToken = UUID.randomUUID().toString()
                        try {
                            var acquired = false
                            while (!acquired) {
                                acquired = stringRedisTemplate.opsForValue()
                                    .setIfAbsent(spinLockKey, threadToken, Duration.ofMillis(500)) == true
                                if (!acquired) {
                                    spinRetryCount.incrementAndGet()
                                    Thread.sleep(5)
                                }
                            }
                            try {
                                Thread.sleep(1)
                            } finally {
                                // 남의 락 삭제 방지를 위한 토큰 소유권 검증 후 해제
                                if (stringRedisTemplate.opsForValue().get(spinLockKey) == threadToken) {
                                    stringRedisTemplate.delete(spinLockKey)
                                }
                            }
                        } finally {
                            spinLatch.countDown()
                        }
                    }
                }
                spinLatch.await(30, TimeUnit.SECONDS)
            }
        }
        val spinTps = "%.2f".format((taskCount.toDouble() / spinTime) * 1000)

        println("\n[1] StringRedisTemplate (Lettuce SETNX 스핀락)")
        println("- 소요시간: ${spinTime}ms")
        println("- 총 스핀락 재시도 횟수 (Polling Query Count): ${spinRetryCount.get()} 회")
        println("- 초당 처리량 (TPS): $spinTps TPS")

        // 2. Redisson Pub/Sub Distributed Lock (Watchdog 자동 갱신 활성화)
        val redissonAcquireCount = AtomicLong(0)
        val redissonLatch = CountDownLatch(taskCount)
        val redissonTime = Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            measureTimeMillis {
                repeat(taskCount) {
                    executor.submit {
                        val lock = redissonClient.getLock(redissonLockKey)
                        try {
                            redissonAcquireCount.incrementAndGet()
                            // leaseTime 미지정 -> Redisson Watchdog 자동 갱신 메커니즘 활성화 (기본 30초)
                            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                                try {
                                    Thread.sleep(1)
                                } finally {
                                    if (lock.isHeldByCurrentThread) {
                                        lock.unlock()
                                    }
                                }
                            }
                        } finally {
                            redissonLatch.countDown()
                        }
                    }
                }
                redissonLatch.await(30, TimeUnit.SECONDS)
            }
        }
        val redissonTps = "%.2f".format((taskCount.toDouble() / redissonTime) * 1000)

        println("\n[2] Redisson Pub/Sub 분산 락")
        println("- 소요시간: ${redissonTime}ms")
        println("- 총 락 획득 시도 횟수 (Acquire Count): ${redissonAcquireCount.get()} 회")
        println("- 초당 처리량 (TPS): $redissonTps TPS")
    }
}
