package com.br.itau.login.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;

@Component
public class LoginRateLimiter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    public boolean tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());
        return bucket.tryConsume(1);
    }
}
