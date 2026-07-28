package com.back.global.security.jwt.repository

import com.back.global.security.jwt.TokenHashUtil
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class BlacklistRepository(
    private val redissonClient: RedissonClient
) {
    fun add(accessToken: String, ttl: Duration) {
        val key = key(accessToken)
        redissonClient.getBucket<String>(key, StringCodec.INSTANCE).set(BLACKLISTED_VALUE, ttl)
    }

    fun isBlacklisted(accessToken: String): Boolean =
        redissonClient.getBucket<String>(key(accessToken), StringCodec.INSTANCE).isExists

    private fun key(accessToken: String): String =
        PREFIX + TokenHashUtil.sha256(accessToken)

    companion object {
        private const val PREFIX = "auth:blacklist:"
        private const val BLACKLISTED_VALUE = "blacklisted"
    }
}
