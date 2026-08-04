package com.back.domain.waiting.service

import com.back.domain.waiting.dto.QueueStatusDto
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class WaitingQueueManager(
    private val stringRedisTemplate: StringRedisTemplate
) {

    fun registerWaiting(scheduleId: Long, userId: Long): Long {
        val waitKey = generateWaitKey(scheduleId)
        val seqKey = generateSequenceKey(scheduleId)
        val user = userId.toString()

        stringRedisTemplate.opsForSet().add(ACTIVE_SCHEDULES_KEY, scheduleId.toString())

        val rank: Long? = stringRedisTemplate.execute(
            REGISTER_WAIT_SCRIPT,
            listOf(waitKey, seqKey),
            user
        )

        if (rank == null || rank < 1) {
            throw ServiceException(ErrorCode.WAITING_QUEUE_REGISTER_FAILED)
        }
        return rank
    }

    fun showWaitingRank(scheduleId: Long, userId: Long): Long =
        findWaitingRank(scheduleId, userId)
            ?: throw ServiceException(ErrorCode.WAITING_QUEUE_NOT_FOUND)

    fun findWaitingRank(scheduleId: Long, userId: Long): Long? =
        stringRedisTemplate.opsForZSet()
            .rank(generateWaitKey(scheduleId), userId.toString())
            ?.let { it + 1L }

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

    fun addActiveUser(scheduleId: Long, capacity: Long, batchSize: Int, ttl: Duration): List<QueueAdmission> {
        val waitKey = generateWaitKey(scheduleId)
        val activeKey = generateQueueActiveKey(scheduleId)
        val now = System.currentTimeMillis()
        val expiredAt = now + ttl.toMillis()
        val tokenCandidates = List(batchSize) { UUID.randomUUID().toString() }

        val rawResult: Any? = stringRedisTemplate.execute(
            ADD_ACTIVE_USER_SCRIPT,
            listOf(waitKey, activeKey),
            capacity.toString(),
            batchSize.toString(),
            now.toString(),
            expiredAt.toString(),
            ttl.toMillis().toString(),
            generateActiveTokenKeyPrefix(scheduleId),
            *tokenCandidates.toTypedArray(),
        )

        val values = rawResult as? List<*> ?: return emptyList()
        return values.chunked(2).mapNotNull { admission ->
            val userId = admission.getOrNull(0)?.toString()?.toLongOrNull() ?: return@mapNotNull null
            val entryToken = admission.getOrNull(1)?.toString() ?: return@mapNotNull null
            QueueAdmission(userId, entryToken, expiredAt)
        }
    }

    fun getActiveToken(scheduleId: Long, userId: Long): String? {
        val score = stringRedisTemplate.opsForZSet().score(generateQueueActiveKey(scheduleId), userId.toString())
        if (score == null || score <= System.currentTimeMillis()) {
            return null
        }
        return stringRedisTemplate.opsForValue().get(generateActiveTokenKey(scheduleId, userId))
    }

    fun getConnectionSnapshot(scheduleId: Long, userId: Long): QueueConnectionSnapshot {
        val result = stringRedisTemplate.execute(
            GET_CONNECTION_SNAPSHOT_SCRIPT,
            listOf(
                generateQueueActiveKey(scheduleId),
                generateActiveTokenKey(scheduleId, userId),
                generateWaitKey(scheduleId),
            ),
            userId.toString(),
            System.currentTimeMillis().toString(),
        ) ?: throw ServiceException(ErrorCode.WAITING_QUEUE_REGISTER_FAILED)

        return when (result.firstOrNull()?.toString()) {
            "ACTIVE" -> result.getOrNull(1)?.toString()
                ?.let(QueueConnectionSnapshot::Active)
                ?: throw ServiceException(ErrorCode.WAITING_QUEUE_REGISTER_FAILED)

            "WAITING" -> {
                val rank = result.getOrNull(1)?.toString()?.toLongOrNull()
                val queueNumber = result.getOrNull(2)?.toString()?.toLongOrNull()
                if (rank == null || queueNumber == null) {
                    throw ServiceException(ErrorCode.WAITING_QUEUE_REGISTER_FAILED)
                }
                QueueConnectionSnapshot.Waiting(rank, queueNumber)
            }

            "NOT_REGISTERED" -> QueueConnectionSnapshot.NotRegistered
            else -> throw ServiceException(ErrorCode.WAITING_QUEUE_REGISTER_FAILED)
        }
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

    fun hasWaitingUsers(scheduleId: Long): Boolean =
        (stringRedisTemplate.opsForZSet().size(generateWaitKey(scheduleId)) ?: 0L) > 0L

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

        private val REGISTER_WAIT_LUA = """
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

        private val ADD_ACTIVE_USER_LUA = """
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
            local result = {}
            for i, u in ipairs(users) do
              local token = ARGV[6 + i]
              redis.call('ZADD', KEYS[2], tonumber(ARGV[4]), u)
              redis.call('SET', ARGV[6] .. u, token, 'PX', ARGV[5])
              table.insert(result, u)
              table.insert(result, token)
            end
            return result
        """.trimIndent()

        private val GET_CONNECTION_SNAPSHOT_LUA = """
            local expiresAt = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if expiresAt then
              if tonumber(expiresAt) > tonumber(ARGV[2]) then
                local token = redis.call('GET', KEYS[2])
                if token then
                  return {'ACTIVE', token}
                end
                redis.call('ZREM', KEYS[1], ARGV[1])
              else
                redis.call('ZREM', KEYS[1], ARGV[1])
                redis.call('DEL', KEYS[2])
              end
            end

            local rank = redis.call('ZRANK', KEYS[3], ARGV[1])
            if rank then
              local queueNumber = redis.call('ZSCORE', KEYS[3], ARGV[1])
              return {'WAITING', tostring(rank + 1), tostring(queueNumber)}
            end
            return {'NOT_REGISTERED'}
        """.trimIndent()

        private val REGISTER_WAIT_SCRIPT: RedisScript<Long> = DefaultRedisScript(REGISTER_WAIT_LUA, Long::class.java)

        private val ADD_ACTIVE_USER_SCRIPT: RedisScript<List<*>> =
            DefaultRedisScript(ADD_ACTIVE_USER_LUA, List::class.java)

        private val GET_CONNECTION_SNAPSHOT_SCRIPT: RedisScript<List<*>> =
            DefaultRedisScript(GET_CONNECTION_SNAPSHOT_LUA, List::class.java)

        private fun generateActiveTokenKeyPrefix(scheduleId: Long): String =
            "$ACTIVE_TOKEN_KEY_PREFIX$scheduleId:"

        @JvmStatic
        fun generateActiveTokenKey(scheduleId: Long, userId: Long): String =
            "$ACTIVE_TOKEN_KEY_PREFIX$scheduleId:$userId"

        @JvmStatic
        fun generateQueueActiveKey(scheduleId: Long): String =
            "queue:active:schedule:$scheduleId"
    }
}
