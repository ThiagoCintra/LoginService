package com.br.itau.login.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Distributed rate limiter backed by Redis.
 *
 * <p>Uses a Lua script that atomically increments a counter key and sets its
 * TTL on the first call within a window.  Because Redis executes Lua scripts
 * as a single atomic operation, there is no race between INCR and EXPIRE.
 * The counter is shared across every application instance in a cluster via
 * the common Redis server.
 *
 * <p>On Redis unavailability (script returns {@code null}) the limiter
 * <em>fails open</em> — traffic is allowed through rather than blocking
 * legitimate users due to an infrastructure hiccup.
 */
@Component
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "rate_limit:login:";

    /**
     * Lua script: atomically increment the counter and set TTL on first call.
     * KEYS[1] = rate limit key, ARGV[1] = window duration in seconds.
     * Returns the new counter value.
     */
    private static final RedisScript<Long> INCR_SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) " +
            "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "return c",
            Long.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final int maxRequests;
    private final long windowSeconds;

    public LoginRateLimiter(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${rate-limit.max-requests:5}") int maxRequests,
            @Value("${rate-limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    /**
     * Attempts to consume one token for the given {@code key} (typically a
     * client IP address).
     *
     * @return {@code true} if the request is within the allowed rate,
     *         {@code false} if the limit has been exceeded.
     */
    public boolean tryConsume(String key) {
        String redisKey = KEY_PREFIX + key;
        Long count = redisTemplate.execute(
                INCR_SCRIPT,
                Collections.singletonList(redisKey),
                String.valueOf(windowSeconds));
        // count == null means Redis was unavailable — fail open
        return count == null || count <= maxRequests;
    }
}
