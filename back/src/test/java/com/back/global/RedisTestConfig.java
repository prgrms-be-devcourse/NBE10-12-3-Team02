package com.back.global;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.embedded.RedisServer;

@TestConfiguration
public class RedisTestConfig {
    private static RedisServer redisServer;
    private static int redisPort;

    private static int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            return 6379;
        }
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            if (redisServer == null) {
                redisPort = findFreePort();
                redisServer = new RedisServer(redisPort);
                redisServer.start();
            }
        } catch (Exception e) {
            throw new RuntimeException("내장 레디스 구동 실패", e);
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory("127.0.0.1", redisPort);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @PreDestroy
    public void stopRedis() throws java.io.IOException {
        if (redisServer != null) {
            try {
                redisServer.stop();
            } catch (Exception e) {
            }
        }
    }
}
