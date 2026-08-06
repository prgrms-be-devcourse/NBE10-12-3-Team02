package com.back.global.security.jwt

object RefreshTokenLuaScripts {

    const val ROTATE_SCRIPT = """
        local oldValue = redis.call('HGET', KEYS[1], ARGV[1])

        if not oldValue then
            return 0
        end

        local activeValue = 'A:' .. ARGV[2]
        local usedValue = 'U:' .. ARGV[2]

        if oldValue == usedValue then
            redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[2], ARGV[7])
            return -2
        end

        if oldValue ~= activeValue then
            return -1
        end

        local currentJti = redis.call('HGET', KEYS[1], 'currentJti')
        if currentJti ~= ARGV[1] then
            redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[2], ARGV[7])
            return -2
        end

        redis.call('HSET', KEYS[1],
            ARGV[1], usedValue,
            ARGV[3], 'A:' .. ARGV[4],
            'currentJti', ARGV[3],
            'lastUsedAt', ARGV[6]
        )
        redis.call('EXPIRE', KEYS[1], ARGV[5])
        redis.call('SADD', KEYS[2], ARGV[7])
        redis.call('EXPIRE', KEYS[2], ARGV[5])

        return 1
    """

    const val VERIFY_SCRIPT = """
        local value = redis.call('HGET', KEYS[1], ARGV[1])

        if not value then
            return 0
        end

        if value == 'U:' .. ARGV[2] then
            redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[2], ARGV[3])
            return -2
        end

        if value ~= 'A:' .. ARGV[2] then
            return -1
        end

        redis.call('HSET', KEYS[1], 'lastUsedAt', ARGV[4])
        return 1
    """

    @JvmStatic
    fun rotateScript(): String = ROTATE_SCRIPT.trimIndent()

    @JvmStatic
    fun verifyScript(): String = VERIFY_SCRIPT.trimIndent()
}
