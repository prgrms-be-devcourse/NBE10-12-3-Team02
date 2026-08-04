package com.back.domain.waiting.service

import com.back.global.RedisTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisTestConfig::class)
class WaitingQueueManagerTest {

    @Autowired
    private lateinit var waitingQueueManager: WaitingQueueManager

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @BeforeEach
    fun setUp() {
        val keys = stringRedisTemplate.keys("queue:*")
        if (!keys.isNullOrEmpty()) {
            stringRedisTemplate.delete(keys)
        }
    }

    @Test
    @DisplayName("대기 사용자의 순번과 대기열 번호를 하나의 Lua 실행으로 조회한다")
    fun t1() {
        waitingQueueManager.registerWaiting(SCHEDULE_ID, FIRST_USER_ID)
        waitingQueueManager.registerWaiting(SCHEDULE_ID, SECOND_USER_ID)

        val snapshot = waitingQueueManager.getConnectionSnapshot(SCHEDULE_ID, SECOND_USER_ID)

        assertThat(snapshot).isEqualTo(QueueConnectionSnapshot.Waiting(rank = 2L, myQueueNumber = 2L))
    }

    @Test
    @DisplayName("대기열 이동과 입장 토큰 저장이 원자적으로 완료된다")
    fun t2() {
        waitingQueueManager.registerWaiting(SCHEDULE_ID, FIRST_USER_ID)

        val admissions = waitingQueueManager.addActiveUser(
            SCHEDULE_ID,
            capacity = 1L,
            batchSize = 1,
            ttl = TOKEN_TTL,
        )

        assertThat(admissions).hasSize(1)
        val admission = admissions.single()
        assertThat(admission.userId).isEqualTo(FIRST_USER_ID)
        assertThat(admission.entryToken).isNotBlank()
        assertThat(admission.expiredAt).isGreaterThan(System.currentTimeMillis())
        assertThat(waitingQueueManager.getConnectionSnapshot(SCHEDULE_ID, FIRST_USER_ID))
            .isEqualTo(QueueConnectionSnapshot.Active(admission.entryToken))
        assertThat(stringRedisTemplate.getExpire(WaitingQueueManager.generateActiveTokenKey(SCHEDULE_ID, FIRST_USER_ID)))
            .isGreaterThan(0L)
    }

    @Test
    @DisplayName("ACTIVE 정보에 토큰이 없으면 불완전한 상태를 정리하고 미등록으로 처리한다")
    fun t3() {
        stringRedisTemplate.opsForZSet().add(
            WaitingQueueManager.generateQueueActiveKey(SCHEDULE_ID),
            FIRST_USER_ID.toString(),
            (System.currentTimeMillis() + TOKEN_TTL.toMillis()).toDouble(),
        )

        val snapshot = waitingQueueManager.getConnectionSnapshot(SCHEDULE_ID, FIRST_USER_ID)

        assertThat(snapshot).isEqualTo(QueueConnectionSnapshot.NotRegistered)
        assertThat(waitingQueueManager.hasValidSession(SCHEDULE_ID, FIRST_USER_ID)).isFalse()
    }

    @Test
    @DisplayName("대기 및 입장 정보가 없으면 미등록 상태를 반환한다")
    fun t4() {
        val snapshot = waitingQueueManager.getConnectionSnapshot(SCHEDULE_ID, FIRST_USER_ID)

        assertThat(snapshot).isEqualTo(QueueConnectionSnapshot.NotRegistered)
    }

    @Test
    @DisplayName("비활성 회차를 정리하면 다음 대기열의 sequence가 1부터 다시 시작한다")
    fun t5() {
        waitingQueueManager.registerWaiting(SCHEDULE_ID, FIRST_USER_ID)
        waitingQueueManager.cancelWaiting(SCHEDULE_ID, FIRST_USER_ID)

        waitingQueueManager.clearInactiveSchedule(SCHEDULE_ID)
        waitingQueueManager.registerWaiting(SCHEDULE_ID, SECOND_USER_ID)

        assertThat(waitingQueueManager.getQueueSequence(SCHEDULE_ID, SECOND_USER_ID)).isEqualTo(1L)
    }

    companion object {
        private const val SCHEDULE_ID = 10L
        private const val FIRST_USER_ID = 101L
        private const val SECOND_USER_ID = 102L
        private val TOKEN_TTL = Duration.ofMinutes(10)
    }
}
