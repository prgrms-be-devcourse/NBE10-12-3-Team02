package com.back.global

import jakarta.annotation.PreDestroy
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import redis.embedded.RedisServer
import java.net.ServerSocket
import java.net.Socket

@TestConfiguration
class RedisTestConfig {

    @Bean
    @Primary
    fun redisConnectionFactory(): RedisConnectionFactory {
        if (isPortActive(6379)) {
            redisPort = 6379
        } else {
            try {
                if (redisServer == null) {
                    redisPort = findFreePort()
                    redisServer = RedisServer(redisPort)
                    redisServer?.start()
                }
            } catch (e: Exception) {
                throw RuntimeException("내장 레디스 구동 실패", e)
            }
        }

        return LettuceConnectionFactory("127.0.0.1", redisPort)
    }

    @Bean
    @Primary
    fun stringRedisTemplate(connectionFactory: RedisConnectionFactory): StringRedisTemplate {
        return StringRedisTemplate().apply {
            setConnectionFactory(connectionFactory)
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = StringRedisSerializer()
            afterPropertiesSet()
        }
    }

    @PreDestroy
    fun stopRedis() {
        if (redisServer != null) {
            try {
                redisServer?.stop()
            } catch (ignored: Exception) {
            }
        }
    }

    companion object {
        private var redisServer: RedisServer? = null
        private var redisPort: Int = 0

        private fun findFreePort(): Int {
            return try {
                ServerSocket(0).use { it.localPort }
            } catch (e: Exception) {
                6370
            }
        }

        private fun isPortActive(port: Int): Boolean {
            return try {
                Socket("127.0.0.1", port).use { true }
            } catch (e: Exception) {
                false
            }
        }
    }
}
