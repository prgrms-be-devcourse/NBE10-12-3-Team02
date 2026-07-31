package com.back.global.security.jwt.repository

import com.back.global.security.jwt.TokenHashUtil
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class BlacklistRepository(
    private val stringRedisTemplate: StringRedisTemplate
) {
    fun add(accessToken: String, ttl: Duration) {
        val key = key(accessToken)
        stringRedisTemplate.opsForValue().set(key, BLACKLISTED_VALUE, ttl)
    }

    fun isBlacklisted(accessToken: String): Boolean =
        stringRedisTemplate.hasKey(key(accessToken)) == true

    private fun key(accessToken: String): String =
        PREFIX + TokenHashUtil.sha256(accessToken)

    companion object {
        private const val PREFIX = "auth:blacklist:"
        private const val BLACKLISTED_VALUE = "blacklisted"
    }
}
