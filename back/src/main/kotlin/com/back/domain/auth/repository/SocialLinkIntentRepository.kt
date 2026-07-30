package com.back.domain.auth.repository

import com.back.domain.user.constant.LoginType
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class SocialLinkIntentRepository(
    private val redissonClient: RedissonClient,
    @Value("\${custom.oauth2.link-intent-prefix:auth:oauth2:link:}")
    private val keyPrefix: String,
) {
    fun save(intentId: String, userId: Long, provider: LoginType, ttl: Duration) {
        redissonClient.getBucket<String>(key(intentId), StringCodec.INSTANCE)
            .set("${provider.name}:$userId", ttl)
    }

    fun consume(intentId: String): SocialLinkIntent? {
        val value = redissonClient.getBucket<String>(key(intentId), StringCodec.INSTANCE)
            .getAndDelete()
            ?: return null

        val parts = value.split(":", limit = 2)
        if (parts.size != 2) return null

        val provider = runCatching { LoginType.valueOf(parts[0]) }.getOrNull() ?: return null
        val userId = parts[1].toLongOrNull() ?: return null
        return SocialLinkIntent(userId, provider)
    }

    private fun key(intentId: String): String = "$keyPrefix$intentId"
}

data class SocialLinkIntent(
    val userId: Long,
    val provider: LoginType,
)
