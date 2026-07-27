package com.back.global.config

import org.redisson.api.RedissonClient
import org.redisson.spring.data.connection.RedissonConnectionFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory::class)
    fun redisConnectionFactory(redissonClient: RedissonClient): RedissonConnectionFactory {
        return RedissonConnectionFactory(redissonClient)
    }

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate::class)
    fun stringRedisTemplate(redisConnectionFactory: RedisConnectionFactory): StringRedisTemplate =
        StringRedisTemplate(redisConnectionFactory)

    @Bean
    @ConditionalOnMissingBean(name = ["redisTemplate"])
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>().apply {
            connectionFactory = redisConnectionFactory
            keySerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            afterPropertiesSet()
        }
}
