package com.back.domain.waiting.service

import com.back.domain.waiting.dto.QueueStatusDto
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class WaitingQueueManager(
    private val stringRedisTemplate: StringRedisTemplate
) {

    fun registerWaiting(scheduleId: Long, userId: Long): Long {
        val waitKey = generateWaitKey(scheduleId)
        val seqKey = generateSequenceKey(scheduleId)
        val user = userId.toString()

        stringRedisTemplate.opsForSet().add(ACTIVE_SCHEDULES_KEY, scheduleId.toString())

        val script = DefaultRedisScript(REGISTER_WAIT_SCRIPT, Long::class.java)
        val rank: Long? = stringRedisTemplate.execute(
            script,
            listOf(waitKey, seqKey),
            user
        )

        if (rank == null || rank < 1) {
            throw ServiceException(ErrorCode.WAITING_QUEUE_REGISTER_FAILED)
        }
        return rank
    }

    fun showWaitingRank(scheduleId: Long, userId: Long): Long =
        stringRedisTemplate.opsForZSet()
            .rank(generateWaitKey(scheduleId), userId.toString())
            ?.let { it + 1L }
            ?: throw ServiceException(ErrorCode.WAITING_QUEUE_NOT_FOUND)

    fun cancelWaiting(scheduleId: Long, userId: Long): Boolean =
        (stringRedisTemplate.opsForZSet().remove(generateWaitKey(scheduleId), userId.toString()) ?: 0L) > 0L

    fun cancelActiveUser(scheduleId: Long, userId: Long): Boolean =
        deleteActiveUser(scheduleId, userId)

    fun removeActiveUser(scheduleId: Long, userId: Long) {
        deleteActiveUser(scheduleId, userId)
    }

    private fun deleteActiveUser(scheduleId: Long, userId: Long): Boolean {
        val removed = (stringRedisTemplate.opsForZSet().remove(generateQueueActiveKey(scheduleId), userId.toString()) ?: 0L) > 0L
        stringRedisTemplate.delete(generateActiveTokenKey(scheduleId, userId))
        return removed
    }

    fun hasValidSession(scheduleId: Long, userId: Long): Boolean {
        val expiresAt = stringRedisTemplate.opsForZSet()
            .score(generateQueueActiveKey(scheduleId), userId.toString()) ?: return false
        return expiresAt >= System.currentTimeMillis()
    }

    fun getStoredToken(scheduleId: Long, userId: Long): String? =
        stringRedisTemplate.opsForValue().get(generateActiveTokenKey(scheduleId, userId))

    fun removeExpiredActiveUsers(scheduleId: Long): Long {
        val removed = stringRedisTemplate.opsForZSet()
            .removeRangeByScore(generateQueueActiveKey(scheduleId), 0.0, System.currentTimeMillis().toDouble()) ?: 0L
        return removed
    }

    @Suppress("UNCHECKED_CAST")
    fun addActiveUser(scheduleId: Long, capacity: Long, batchSize: Int, ttl: Duration): List<Long> {
        val waitKey = generateWaitKey(scheduleId)
        val activeKey = generateQueueActiveKey(scheduleId)
        val now = System.currentTimeMillis()
        val expiredAt = now + ttl.toMillis()

        val script = DefaultRedisScript(ADD_ACTIVE_USER_SCRIPT, List::class.java)
        val userIds: List<String>? = stringRedisTemplate.execute(
            script,
            listOf(waitKey, activeKey),
            capacity.toString(),
            batchSize.toString(),
            now.toString(),
            expiredAt.toString()
        ) as? List<String>

        if (userIds.isNullOrEmpty()) {
            return emptyList()
        }
        return userIds.map { it.toLong() }
    }

    fun issueToken(scheduleId: Long, userId: Long, ttl: Duration): String {
        val entryToken = java.util.UUID.randomUUID().toString()
        stringRedisTemplate.opsForValue().set(generateActiveTokenKey(scheduleId, userId), entryToken, ttl)
        return entryToken
    }

    fun getActiveToken(scheduleId: Long, userId: Long): String? {
        val score = stringRedisTemplate.opsForZSet().score(generateQueueActiveKey(scheduleId), userId.toString())
        if (score == null || score <= System.currentTimeMillis()) {
            return null
        }
        return stringRedisTemplate.opsForValue().get(generateActiveTokenKey(scheduleId, userId))
    }

    fun getQueueStatus(scheduleId: Long): QueueStatusDto {
        val waitKey = generateWaitKey(scheduleId)
        val totalWaitingCount = stringRedisTemplate.opsForZSet().size(waitKey) ?: 0L
        if (totalWaitingCount == 0L) {
            return QueueStatusDto(0L, 0L)
        }

        val rangeWithScores = stringRedisTemplate.opsForZSet().rangeWithScores(waitKey, 0, 0)
        val firstScore = rangeWithScores?.firstOrNull()?.score
        val currentAllowedSequence = if (firstScore != null) firstScore.toLong() - 1 else 0L
        return QueueStatusDto(currentAllowedSequence, totalWaitingCount)
    }

    fun getQueueSequence(scheduleId: Long, userId: Long): Long {
        val score = stringRedisTemplate.opsForZSet().score(generateWaitKey(scheduleId), userId.toString())
        return score?.toLong() ?: 0L
    }

    fun isQueueEmpty(scheduleId: Long): Boolean {
        val waitSize = stringRedisTemplate.opsForZSet().size(generateWaitKey(scheduleId)) ?: 0L
        val activeSize = stringRedisTemplate.opsForZSet().size(generateQueueActiveKey(scheduleId)) ?: 0L
        return waitSize == 0L && activeSize == 0L
    }

    fun clearWaitingQueue(scheduleId: Long) {
        stringRedisTemplate.delete(generateWaitKey(scheduleId))
    }

    fun getActiveScheduleIds(): Set<String> {
        return stringRedisTemplate.opsForSet().members(ACTIVE_SCHEDULES_KEY) ?: emptySet()
    }

    fun removeFromActiveSchedules(scheduleIdStr: String) {
        stringRedisTemplate.opsForSet().remove(ACTIVE_SCHEDULES_KEY, scheduleIdStr)
    }

    private fun generateWaitKey(scheduleId: Long): String = "$WAIT_KEY_PREFIX$scheduleId"
    private fun generateSequenceKey(scheduleId: Long): String = "$SEQUENCE_KEY_PREFIX$scheduleId"

    companion object {
        private const val WAIT_KEY_PREFIX = "queue:wait:schedule:"
        private const val SEQUENCE_KEY_PREFIX = "queue:wait:sequence:schedule:"
        private const val ACTIVE_TOKEN_KEY_PREFIX = "queue:active:token:"
        private const val ACTIVE_SCHEDULES_KEY = "queue:active:schedules"

        private val REGISTER_WAIT_SCRIPT = """
            local exists = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if not exists then
              local sequence = redis.call('INCR', KEYS[2])
              redis.call('ZADD', KEYS[1], sequence, ARGV[1])
            end
            local rank = redis.call('ZRANK', KEYS[1], ARGV[1])
            if not rank then
              return -1
            end
            return rank + 1
        """.trimIndent()

        private val ADD_ACTIVE_USER_SCRIPT = """
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', tonumber(ARGV[3]))
            local activeCount = redis.call('ZCARD', KEYS[2])
            local availableSlots = tonumber(ARGV[1]) - activeCount
            if availableSlots <= 0 then
              return {}
            end
            local popCount = math.min(availableSlots, tonumber(ARGV[2]))
            local users = redis.call('ZRANGE', KEYS[1], 0, popCount - 1)
            if #users == 0 then
              return {}
            end
            redis.call('ZREM', KEYS[1], unpack(users))
            for _, u in ipairs(users) do
              redis.call('ZADD', KEYS[2], tonumber(ARGV[4]), u)
            end
            return users
        """.trimIndent()

        @JvmStatic
        fun generateActiveTokenKey(scheduleId: Long, userId: Long): String =
            "$ACTIVE_TOKEN_KEY_PREFIX$scheduleId:$userId"

        @JvmStatic
        fun generateQueueActiveKey(scheduleId: Long): String =
            "queue:active:schedule:$scheduleId"
    }
}
