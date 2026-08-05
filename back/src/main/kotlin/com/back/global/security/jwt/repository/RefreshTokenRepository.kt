package com.back.global.security.jwt.repository

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.security.jwt.RefreshTokenLuaScripts
import com.back.global.security.jwt.constant.RefreshTokenKeyType
import com.back.global.security.jwt.constant.RefreshTokenValidationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant

@Repository
class RefreshTokenRepository(
    private val stringRedisTemplate: StringRedisTemplate,
    @Value("\${custom.redis.refresh-token.prefix}")
    private val prefix: String,
    @Value("\${custom.redis.refresh-token.index-prefix}")
    private val indexPrefix: String,
) {
    fun rotate(
        userId: Long,
        sessionId: String,
        oldJti: String,
        requestRefreshTokenHash: String,
        newJti: String,
        newRefreshTokenHash: String,
        ttl: Duration,
    ): RefreshTokenValidationResult {
        val result = stringRedisTemplate.execute(
            ROTATE_SCRIPT,
            listOf(
                generateKey(RefreshTokenKeyType.FAMILY, userId, sessionId),
                generateKey(RefreshTokenKeyType.INDEX, userId),
            ),
            oldJti,
            requestRefreshTokenHash,
            newJti,
            newRefreshTokenHash,
            ttl.toSeconds().toString(),
            Instant.now().toEpochMilli().toString(),
            sessionId,
        ) ?: throw ServiceException(ErrorCode.AUTH_REFRESH_TOKEN_ROTATION_FAILED)

        return validationResult(result)
    }

    fun save(
        userId: Long,
        sessionId: String,
        jti: String,
        refreshTokenHash: String,
        ttl: Duration,
    ) {
        val familyKey = generateKey(RefreshTokenKeyType.FAMILY, userId, sessionId)
        val indexKey = generateKey(RefreshTokenKeyType.INDEX, userId)
        val now = Instant.now().toEpochMilli().toString()

        stringRedisTemplate.opsForHash<String, String>().putAll(
            familyKey,
            mapOf(
                jti to "$ACTIVE_PREFIX$refreshTokenHash",
                CURRENT_JTI_FIELD to jti,
                CREATED_AT_FIELD to now,
                LAST_USED_AT_FIELD to now,
            ),
        )
        stringRedisTemplate.expire(familyKey, ttl)
        stringRedisTemplate.opsForSet().add(indexKey, sessionId)
        stringRedisTemplate.expire(indexKey, ttl)
    }

    fun verify(
        userId: Long,
        sessionId: String,
        jti: String,
        requestRefreshTokenHash: String,
    ): RefreshTokenValidationResult {
        val result = stringRedisTemplate.execute(
            VERIFY_SCRIPT,
            listOf(
                generateKey(RefreshTokenKeyType.FAMILY, userId, sessionId),
                generateKey(RefreshTokenKeyType.INDEX, userId),
            ),
            jti,
            requestRefreshTokenHash,
            sessionId,
            Instant.now().toEpochMilli().toString(),
        ) ?: throw ServiceException(ErrorCode.AUTH_REFRESH_TOKEN_ROTATION_FAILED)

        return validationResult(result)
    }

    fun deleteFamily(userId: Long, sessionId: String) {
        stringRedisTemplate.delete(generateKey(RefreshTokenKeyType.FAMILY, userId, sessionId))
        stringRedisTemplate.opsForSet().remove(generateKey(RefreshTokenKeyType.INDEX, userId), sessionId)
    }

    fun deleteAllByUserId(userId: Long) {
        val indexKey = generateKey(RefreshTokenKeyType.INDEX, userId)
        val sessionIds = stringRedisTemplate.opsForSet().members(indexKey) ?: emptySet()

        if (sessionIds.isNotEmpty()) {
            stringRedisTemplate.delete(
                sessionIds.map { sessionId ->
                    generateKey(RefreshTokenKeyType.FAMILY, userId, sessionId)
                },
            )
        }
        stringRedisTemplate.delete(indexKey)
    }

    private fun generateKey(type: RefreshTokenKeyType, userId: Long, sessionId: String? = null): String =
        when (type) {
            RefreshTokenKeyType.FAMILY -> {
                require(!sessionId.isNullOrBlank()) { "sessionId is required for refresh token family key" }
                "${prefix}family:$userId:$sessionId"
            }

            RefreshTokenKeyType.INDEX -> {
                require(sessionId.isNullOrBlank()) { "sessionId must be empty for refresh token index key" }
                "$indexPrefix$userId"
            }
        }

    private fun validationResult(result: Long): RefreshTokenValidationResult =
        when (result.toInt()) {
            1 -> RefreshTokenValidationResult.SUCCESS
            0 -> RefreshTokenValidationResult.NOT_FOUND
            -1 -> RefreshTokenValidationResult.MISMATCH
            -2 -> RefreshTokenValidationResult.REUSED
            else -> throw ServiceException(ErrorCode.AUTH_REFRESH_TOKEN_ROTATION_FAILED)
        }

    companion object {
        private const val ACTIVE_PREFIX = "A:"
        private const val CURRENT_JTI_FIELD = "currentJti"
        private const val CREATED_AT_FIELD = "createdAt"
        private const val LAST_USED_AT_FIELD = "lastUsedAt"

        private val ROTATE_SCRIPT: RedisScript<Long> =
            DefaultRedisScript(RefreshTokenLuaScripts.rotateScript(), Long::class.java)
        private val VERIFY_SCRIPT: RedisScript<Long> =
            DefaultRedisScript(RefreshTokenLuaScripts.verifyScript(), Long::class.java)
    }
}
