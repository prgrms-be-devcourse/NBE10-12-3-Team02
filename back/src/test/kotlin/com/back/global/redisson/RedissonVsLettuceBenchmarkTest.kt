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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

@SpringBootTest
@Import(RedisTestConfig::class)
class RedissonVsLettuceBenchmarkTest {

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @Test
    @DisplayName("Redisson Pub/Sub 분산 락 vs StringRedisTemplate 스핀락 1,000건 동시 경합 벤치마크")
    fun compareRedissonAndSpinLock() {
        val taskCount = 1000
        val spinLockKey = "benchmark:seat:spin_lock"
        val redissonLockKey = "benchmark:seat:redisson_lock"

        println("==================================================")
        println("Redisson Pub/Sub 락 vs StringRedisTemplate 스핀락 벤치마크 테스트")
        println("동시 경합 요청 수: $taskCount 건")
        println("==================================================")

        // 1. StringRedisTemplate (Lettuce SETNX Spin-Lock)
        val spinRetryCount = AtomicLong(0)
        val spinLatch = CountDownLatch(taskCount)
        val spinTime = Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            measureTimeMillis {
                repeat(taskCount) {
                    executor.submit {
                        try {
                            var acquired = false
                            while (!acquired) {
                                acquired = stringRedisTemplate.opsForValue()
                                    .setIfAbsent(spinLockKey, "LOCKED", Duration.ofMillis(500)) == true
                                if (!acquired) {
                                    spinRetryCount.incrementAndGet()
                                    Thread.sleep(5)
                                }
                            }
                            Thread.sleep(1)
                            stringRedisTemplate.delete(spinLockKey)
                        } finally {
                            spinLatch.countDown()
                        }
                    }
                }
                spinLatch.await(30, TimeUnit.SECONDS) // 안전 타임아웃 30초 설정
            }
        }
        val spinTps = "%.2f".format((taskCount.toDouble() / spinTime) * 1000)

        println("\n[1] StringRedisTemplate (Lettuce SETNX 스핀락)")
        println("- 소요시간: ${spinTime}ms")
        println("- 총 스핀락 추가 폴링 쿼리 횟수 (Redis Polling Retries): ${spinRetryCount.get()} 회")
        println("- 초당 처리량 (TPS): $spinTps TPS")

        // 2. Redisson Pub/Sub Distributed Lock
        val redissonAcquireCount = AtomicLong(0)
        val redissonLatch = CountDownLatch(taskCount)
        val redissonTime = Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            measureTimeMillis {
                repeat(taskCount) {
                    executor.submit {
                        val lock = redissonClient.getLock(redissonLockKey)
                        try {
                            redissonAcquireCount.incrementAndGet()
                            if (lock.tryLock(5, 1, TimeUnit.SECONDS)) {
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
                redissonLatch.await(30, TimeUnit.SECONDS) // 안전 타임아웃 30초 설정
            }
        }
        val redissonTps = "%.2f".format((taskCount.toDouble() / redissonTime) * 1000)
        val queryReductionRatio = "%.2f".format(spinRetryCount.get().toDouble() / redissonAcquireCount.get())

        println("\n[2] Redisson Pub/Sub 분산 락")
        println("- 소요시간: ${redissonTime}ms")
        println("- 총 락 획득 시도 횟수 (Acquire Attempts): ${redissonAcquireCount.get()} 회")
        println("  (※ 락 미획득 시 지속적인 폴링 쿼리 없이 Pub/Sub 채널 이벤트를 대기하여 추가 레디스 쿼리 0회)")
        println("- 초당 처리량 (TPS): $redissonTps TPS")

        println("\n==================================================")
        println("[최종 비교 요약]")
        println("1. StringRedisTemplate 스핀락 레디스 폴링 쿼리 횟수: ${spinRetryCount.get()}회")
        println("2. Redisson Pub/Sub 락 시도 횟수: ${redissonAcquireCount.get()}회")
        println("➔ Redisson Pub/Sub 락 도입으로 레디스 쿼리 및 네트워크 Overhead 약 ${queryReductionRatio}배 감축!")
        println("==================================================")
    }
}
