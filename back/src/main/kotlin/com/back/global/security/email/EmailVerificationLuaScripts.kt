package com.back.global.security.email

object EmailVerificationLuaScripts {
    val SAVE_CHALLENGE_SCRIPT = """
        local cooldownCreated = redis.call(
            'SET',
            KEYS[1],
            ARGV[1],
            'PX',
            ARGV[2],
            'NX'
        )

        if not cooldownCreated then
            return 0
        end

        redis.call('SET', KEYS[2], ARGV[3], 'PX', ARGV[4])
        redis.call('DEL', KEYS[3])
        return 1
    """.trimIndent()

    val CONFIRM_SCRIPT = """
        local savedCodeHash = redis.call('GET', KEYS[1])

        if not savedCodeHash then
            return 0
        end

        local attempts = redis.call('INCR', KEYS[2])
        if attempts == 1 then
            local codeTtl = redis.call('PTTL', KEYS[1])
            if codeTtl > 0 then
                redis.call('PEXPIRE', KEYS[2], codeTtl)
            end
        end

        if attempts > tonumber(ARGV[2]) then
            redis.call('DEL', KEYS[1], KEYS[2])
            return -2
        end

        if savedCodeHash ~= ARGV[1] then
            return -1
        end

        redis.call('SET', KEYS[3], ARGV[3], 'PX', ARGV[4])
        redis.call('DEL', KEYS[1], KEYS[2])
        return 1
    """.trimIndent()

    val RESERVE_SCRIPT = """
        local value = redis.call('GET', KEYS[1])
        if value ~= ARGV[1] then
            return 0
        end

        redis.call('SET', KEYS[1], ARGV[2], 'KEEPTTL')
        return 1
    """.trimIndent()

    val COMPLETE_RESERVATION_SCRIPT = """
        local value = redis.call('GET', KEYS[1])
        if value ~= ARGV[1] then
            return 0
        end

        redis.call('DEL', KEYS[1])
        return 1
    """.trimIndent()

    val RESTORE_RESERVATION_SCRIPT = """
        local value = redis.call('GET', KEYS[1])
        if value ~= ARGV[1] then
            return 0
        end

        redis.call('SET', KEYS[1], ARGV[2], 'KEEPTTL')
        return 1
    """.trimIndent()

    val CLEAR_CHALLENGE_SCRIPT = """
        redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
        return 1
    """.trimIndent()

    @JvmStatic
    fun saveChallengeScript(): String = SAVE_CHALLENGE_SCRIPT

    @JvmStatic
    fun confirmScript(): String = CONFIRM_SCRIPT

    @JvmStatic
    fun reserveScript(): String = RESERVE_SCRIPT

    @JvmStatic
    fun completeReservationScript(): String = COMPLETE_RESERVATION_SCRIPT

    @JvmStatic
    fun restoreReservationScript(): String = RESTORE_RESERVATION_SCRIPT

    @JvmStatic
    fun clearChallengeScript(): String = CLEAR_CHALLENGE_SCRIPT
}
