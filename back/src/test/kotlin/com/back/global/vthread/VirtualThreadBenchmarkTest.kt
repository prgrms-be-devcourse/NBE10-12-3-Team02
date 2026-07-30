package com.back.global.vthread

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

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

        val speedup = String.format("%.2f", platformTime.toDouble() / virtualTime)
        println("\n==================================================")
        println("[최종 벤치마크 요약]")
        println("가상 스레드가 플랫폼 스레드 대비 약 ${speedup}배 빠른 처리 속도를 보였습니다.")
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

        val time = executorSupplier().use { executor ->
            measureTimeMillis {
                repeat(taskCount) {
                    executor.submit {
                        try {
                            // Non-blocking I/O대기 시뮬레이션
                            Thread.sleep(ioDelayMs)
                            completedCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                latch.await()
            }
        }

        val tps = String.format("%.2f", (completedCount.get().toDouble() / time) * 1000)
        println("\n[$name]")
        println("- 총 완료 작업 수: ${completedCount.get()} / $taskCount")
        println("- 총 소요 시간: ${time}ms")
        println("- 초당 처리량 (TPS): $tps TPS")

        return time
    }
}
