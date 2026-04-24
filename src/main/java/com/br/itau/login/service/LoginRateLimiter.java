package com.br.itau.login.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Distributed rate limiter backed by Redis.
 *
 * <p>Uses the Redis {@code INCR} + {@code EXPIRE} pattern so the counter is
 * shared across every application instance in a cluster.  The first increment
 * in a window also sets the key TTL, so the window self-resets automatically.
 *
 * <p>On Redis unavailability (counter returns {@code null}) the limiter
 * <em>fails open</em> — traffic is allowed through rather than blocking
 * legitimate users due to an infrastructure hiccup.
 */
@Component
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "rate_limit:login:";

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
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == null) {
            // Redis unavailable — fail open to avoid blocking legitimate traffic
            return true;
        }
        if (count == 1L) {
            // First request in this window: attach a TTL so the key expires automatically
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }
        return count <= maxRequests;
    }
}
