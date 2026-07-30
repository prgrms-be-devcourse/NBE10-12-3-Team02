package com.back.global.vthread

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false", "logging.level.org.hibernate.SQL=OFF"])
class VirtualThreadBenchmarkTest {

    @Test
    @DisplayName("가상 스레드 vs 플랫폼 스레드 동시성 I/O 1,000건 벤치마크 비교")
    fun compareVirtualAndPlatformThreads() {
        val taskCount = 1000
        val ioDelayMs = 50L // 50ms I/O blocking 대기 시뮬레이션

        println("==================================================")
        println("가상 스레드 vs 플랫폼 스레드 벤치마크 테스트 시작")
        println("총 작업 수: $taskCount 건, 각 I/O 대기시간: ${ioDelayMs}ms")
        println("==================================================")

        // 1. 플랫폼 스레드 풀 (FixedThreadPool: 200)
        val platformTime = runBenchmark("Platform ThreadPool (Fixed 200)", taskCount, ioDelayMs) {
            Executors.newFixedThreadPool(200)
        }

        // 2. 가상 스레드 (VirtualThreadPerTaskExecutor)
        val virtualTime = runBenchmark("Virtual Thread (VirtualThreadPerTask)", taskCount, ioDelayMs) {
            Executors.newVirtualThreadPerTaskExecutor()
        }

        println("\n==================================================")
        println("[성능 비교 측정 결과]")
        println("1. Platform ThreadPool (200개 제한): ${platformTime}ms")
        println("2. Virtual Thread (VirtualThreadPerTask): ${virtualTime}ms")
        println("==================================================")
    }

    private fun runBenchmark(
        name: String,
        taskCount: Int,
        ioDelayMs: Long,
        executorSupplier: () -> java.util.concurrent.ExecutorService
    ): Long {
        val latch = CountDownLatch(taskCount)
        val completedCount = AtomicLong(0)

        val totalTime = executorSupplier().use { executor ->
            measureTimeMillis {
                repeat(taskCount) {
                    executor.submit {
                        try {
                            Thread.sleep(ioDelayMs) // I/O 블로킹 대기 시뮬레이션
                            completedCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                latch.await()
            }
        }

        val tps = String.format("%.2f", (taskCount.toDouble() / totalTime) * 1000)
        println("[$name] 소요시간: ${totalTime}ms, 완료 작업: ${completedCount.get()}건, 처리량: ${tps} TPS")
        return totalTime
    }
}
