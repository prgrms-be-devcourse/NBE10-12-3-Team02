package com.back.global.security.jwt.repository

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.security.jwt.RefreshTokenLuaScripts
import com.back.global.security.jwt.constant.RefreshTokenKeyType
import com.back.global.security.jwt.constant.RefreshTokenValidationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import java.time.Duration

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
        oldJti: String,
        requestRefreshTokenHash: String,
        newJti: String,
        newRefreshTokenHash: String,
        ttl: Duration,
    ): RefreshTokenValidationResult {
        val result = stringRedisTemplate.execute(
            ROTATE_REDIS_SCRIPT,
            listOf(
                generateKey(RefreshTokenKeyType.TOKEN, userId, oldJti),
                generateKey(RefreshTokenKeyType.TOKEN, userId, newJti),
                generateKey(RefreshTokenKeyType.INDEX, userId),
            ),
            requestRefreshTokenHash,
            newRefreshTokenHash,
            ttl.toSeconds().toString(),
            oldJti,
            newJti,
        ) ?: throw ServiceException(ErrorCode.AUTH_REFRESH_TOKEN_ROTATION_FAILED)

        return when (result.toInt()) {
            1 -> RefreshTokenValidationResult.SUCCESS
            -1 -> RefreshTokenValidationResult.MISMATCH
            0 -> RefreshTokenValidationResult.NOT_FOUND
            else -> throw ServiceException(ErrorCode.AUTH_REFRESH_TOKEN_ROTATION_FAILED)
        }
    }

    fun save(userId: Long, jti: String, refreshTokenHash: String, ttl: Duration) {
        val tokenKey = generateKey(RefreshTokenKeyType.TOKEN, userId, jti)
        val indexKey = generateKey(RefreshTokenKeyType.INDEX, userId)

        stringRedisTemplate.opsForValue().set(tokenKey, refreshTokenHash, ttl)
        stringRedisTemplate.opsForSet().add(indexKey, jti)
        stringRedisTemplate.expire(indexKey, ttl)
    }

    fun verify(userId: Long, jti: String, requestRefreshTokenHash: String): RefreshTokenValidationResult {
        val tokenKey = generateKey(RefreshTokenKeyType.TOKEN, userId, jti)
        val savedHash = stringRedisTemplate.opsForValue().get(tokenKey)

        return when {
            savedHash == null -> RefreshTokenValidationResult.NOT_FOUND
            savedHash != requestRefreshTokenHash -> RefreshTokenValidationResult.MISMATCH
            else -> RefreshTokenValidationResult.SUCCESS
        }
    }

    fun delete(userId: Long, jti: String) {
        val tokenKey = generateKey(RefreshTokenKeyType.TOKEN, userId, jti)
        val indexKey = generateKey(RefreshTokenKeyType.INDEX, userId)

        stringRedisTemplate.delete(tokenKey)
        stringRedisTemplate.opsForSet().remove(indexKey, jti)
    }

    fun deleteAllByUserId(userId: Long) {
        val indexKey = generateKey(RefreshTokenKeyType.INDEX, userId)
        val jtis = stringRedisTemplate.opsForSet().members(indexKey) ?: emptySet()
        if (jtis.isEmpty()) return

        val keysToDelete = jtis.map { jti -> generateKey(RefreshTokenKeyType.TOKEN, userId, jti) }
        stringRedisTemplate.delete(keysToDelete)
        stringRedisTemplate.delete(indexKey)
    }

    private fun generateKey(type: RefreshTokenKeyType, userId: Long, jti: String? = null): String =
        when (type) {
            RefreshTokenKeyType.TOKEN -> {
                require(!jti.isNullOrBlank()) { "jti is required for refresh token key" }
                "$prefix$userId:$jti"
            }

            RefreshTokenKeyType.INDEX -> {
                require(jti.isNullOrBlank()) { "jti must be empty for refresh token index key" }
                "$indexPrefix$userId"
            }
        }

    companion object {
        private val ROTATE_REDIS_SCRIPT = DefaultRedisScript(RefreshTokenLuaScripts.rotateScript(), Long::class.java)
    }
}
