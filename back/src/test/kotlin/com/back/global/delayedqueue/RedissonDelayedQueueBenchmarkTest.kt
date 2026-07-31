package com.back.global.delayedqueue

import com.back.global.RedisTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * Redisson Delayed Queue 기능 검증 및 성능 벤치마크 테스트
 *
 * [검증 시나리오]
 * 1. 만료 정밀도 테스트: offer() 후 지정된 TTL(초) 내에 이벤트가 정확히 수신되는지 검증
 * 2. 대량 처리 처리량 테스트: 100건을 동시 offer() 후 전원 이벤트 수신까지 소요 시간 측정
 * 3. 중복 만료 안전성 테스트: 같은 메시지를 다수 offer() 했을 때 중복 처리 여부 검증
 */
@SpringBootTest(properties = [
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.format_sql=false",
    "logging.level.org.hibernate.SQL=OFF",
    // SeatHoldExpiredHandler가 실제 DB 처리를 시도하지 않도록 핸들러 비활성화
    "seat.hold.handler.enabled=false"
])
@Import(RedisTestConfig::class)
class RedissonDelayedQueueBenchmarkTest {

    @Autowired
    private lateinit var redissonClient: RedissonClient

    private lateinit var testQueueKey: String

    @BeforeEach
    fun setUp() {
        // [개선 1] 테스트별 고유 Key 생성으로 테스트 격리성 및 병렬 실행 독립성 보장
        testQueueKey = "test:delayed:queue:${UUID.randomUUID()}"
    }

    @AfterEach
    fun tearDown() {
        // [개선 2] destroy()를 호출하여 Delayed Queue 내부 스케줄러 태스크 및 레디스 리소스 완전 해제
        if (::testQueueKey.isInitialized) {
            val blockingQueue = redissonClient.getBlockingQueue<String>(testQueueKey, StringCodec.INSTANCE)
            val delayedQueue = redissonClient.getDelayedQueue(blockingQueue)
            delayedQueue.clear()
            delayedQueue.destroy()
            blockingQueue.clear()
        }
    }

    // ==========================================================
    // 시나리오 1: 만료 정밀도 검증
    // ==========================================================
    @Test
    @DisplayName("만료 정밀도: TTL 3초 지정 시 실제 수신까지 오차 500ms 이내인지 검증")
    fun expirationPrecisionTest() {
        val ttlSeconds = 3L
        val message = "1:21:A-1"

        val blockingQueue = redissonClient.getBlockingQueue<String>(testQueueKey, StringCodec.INSTANCE)
        val delayedQueue = redissonClient.getDelayedQueue(blockingQueue)

        println("==================================================")
        println("[시나리오 1] Redisson Delayed Queue 만료 정밀도 검증")
        println("- TTL 지정: ${ttlSeconds}초")
        println("==================================================")

        val offerTime = System.currentTimeMillis()
        delayedQueue.offer(message, ttlSeconds, TimeUnit.SECONDS)
        println("- offer() 완료: ${message}")

        // TTL 만료까지 최대 (TTL + 2초) 대기
        val received = blockingQueue.poll(ttlSeconds + 2, TimeUnit.SECONDS)
        val receiveTime = System.currentTimeMillis()

        val actualDelayMs = receiveTime - offerTime
        val errorMs = actualDelayMs - (ttlSeconds * 1000)

        println("- 수신된 메시지: $received")
        println("- 실제 대기 시간: ${actualDelayMs}ms (TTL=${ttlSeconds * 1000}ms, 오차=${errorMs}ms)")

        assertThat(received).isEqualTo(message)
        assertThat(errorMs).isLessThan(500) // 500ms 이내 오차 허용
        println("✓ 만료 정밀도 검증 통과: 오차 ${errorMs}ms")
    }

    // ==========================================================
    // 시나리오 2: 대량 처리 처리량 벤치마크
    // ==========================================================
    @Test
    @DisplayName("대량 처리 처리량: 100건 동시 offer() 후 전원 이벤트 수신까지 소요 시간 측정")
    fun bulkThroughputBenchmarkTest() {
        val taskCount = 100
        val ttlSeconds = 2L

        println("==================================================")
        println("[시나리오 2] Redisson Delayed Queue 대량 처리 처리량 벤치마크")
        println("- 동시 등록 건수: ${taskCount}건, TTL: ${ttlSeconds}초")
        println("==================================================")

        val blockingQueue = redissonClient.getBlockingQueue<String>(testQueueKey, StringCodec.INSTANCE)
        val delayedQueue = redissonClient.getDelayedQueue(blockingQueue)

        // 1단계: 100건 동시 offer() 성능 측정
        val offerLatch = CountDownLatch(taskCount)
        val offerTime = Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            measureTimeMillis {
                repeat(taskCount) { i ->
                    executor.submit {
                        try {
                            val message = buildTestMessage(i)
                            delayedQueue.offer(message, ttlSeconds, TimeUnit.SECONDS)
                        } finally {
                            offerLatch.countDown()
                        }
                    }
                }
                offerLatch.await(10, TimeUnit.SECONDS)
            }
        }
        val offerTps = "%.2f".format((taskCount.toDouble() / offerTime) * 1000)

        println("\n[등록 성능]")
        println("- ${taskCount}건 offer() 완료 소요시간: ${offerTime}ms")
        println("- 초당 등록 처리량 (TPS): $offerTps TPS")

        // 2단계: 전원 이벤트 수신까지 소요 시간 측정
        val receivedCount = AtomicInteger(0)
        val receiveLatch = CountDownLatch(taskCount)
        val receiveStartTime = System.currentTimeMillis()

        Executors.newSingleThreadExecutor().use { consumerExecutor ->
            consumerExecutor.submit {
                while (receivedCount.get() < taskCount) {
                    val msg = blockingQueue.poll(ttlSeconds + 3, TimeUnit.SECONDS) ?: break
                    receivedCount.incrementAndGet()
                    receiveLatch.countDown()
                }
            }
            // TTL + 여유시간 대기
            receiveLatch.await(ttlSeconds + 5, TimeUnit.SECONDS)
        }

        val totalReceiveTime = System.currentTimeMillis() - receiveStartTime

        println("\n[수신 성능]")
        println("- 수신된 이벤트 수: ${receivedCount.get()} / $taskCount 건")
        println("- 전원 수신 완료 소요시간: ${totalReceiveTime}ms")

        assertThat(receivedCount.get()).isEqualTo(taskCount)
        println("✓ ${taskCount}건 전원 수신 완료")
    }

    // ==========================================================
    // 시나리오 3: 중복 만료 안전성 검증
    // ==========================================================
    @Test
    @DisplayName("중복 만료 안전성: 동일 메시지 다수 offer() 시 수신 횟수가 offer 횟수와 정확히 일치하는지 검증")
    fun duplicateOfferSafetyTest() {
        val duplicateCount = 5
        val ttlSeconds = 2L
        val message = "1:21:A-1" // 동일한 메시지를 여러 번 offer

        println("==================================================")
        println("[시나리오 3] Redisson Delayed Queue 중복 만료 안전성 검증")
        println("- 동일 메시지 반복 등록: ${duplicateCount}회, TTL: ${ttlSeconds}초")
        println("==================================================")

        val blockingQueue = redissonClient.getBlockingQueue<String>(testQueueKey, StringCodec.INSTANCE)
        val delayedQueue = redissonClient.getDelayedQueue(blockingQueue)

        repeat(duplicateCount) {
            delayedQueue.offer(message, ttlSeconds, TimeUnit.SECONDS)
        }
        println("- ${duplicateCount}회 동일 메시지 offer() 완료")

        // [개선 3] poll() 타임아웃을 활용하여 수신 루프 가독성 개선 및 수신 즉시 탈출
        val receivedCount = AtomicInteger(0)
        while (receivedCount.get() < duplicateCount) {
            val msg = blockingQueue.poll(ttlSeconds + 2, TimeUnit.SECONDS) ?: break
            if (msg == message) {
                receivedCount.incrementAndGet()
            }
        }

        println("- 수신된 이벤트 총 횟수: ${receivedCount.get()}")
        println("- 기대 수신 횟수: $duplicateCount")

        // Redisson Delayed Queue는 동일 메시지를 각각 독립 엔트리로 등록하므로
        // offer 횟수만큼 수신이 보장되어야 함
        assertThat(receivedCount.get()).isEqualTo(duplicateCount)
        println("✓ 중복 안전성 검증 통과: ${receivedCount.get()}건 수신 확인")
    }

    private fun buildTestMessage(index: Int): String = "1:21:${'A' + (index % 5)}-${(index % 30) + 1}"
}
