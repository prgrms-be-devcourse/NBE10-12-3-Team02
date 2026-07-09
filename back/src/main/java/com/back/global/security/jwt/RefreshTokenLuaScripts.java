package com.back.global.security.jwt;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshTokenLuaScripts {

    public static final DefaultRedisScript<Long> ROTATE = new DefaultRedisScript<>(
            """
                    local oldValue = redis.call('GET', KEYS[1])
                    
                    if not oldValue then
                        return 0
                    end
                    
                    if oldValue ~= ARGV[1] then
                        return -1
                    end
                    
                    redis.call('SET', KEYS[1], oldValue, 'EX', 5)
                    redis.call('SREM', KEYS[3], ARGV[4])
                    
                    redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])
                    redis.call('SADD', KEYS[3], ARGV[5])
                    redis.call('EXPIRE', KEYS[3], ARGV[3])
                    
                    return 1
                    """,
            Long.class
    );
}
