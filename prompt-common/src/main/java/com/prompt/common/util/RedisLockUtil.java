package com.prompt.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

public class RedisLockUtil {

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_TIMEOUT_SECONDS = 10;

    private final StringRedisTemplate redisTemplate;
    private final String lockKey;
    private final String lockValue;

    public RedisLockUtil(StringRedisTemplate redisTemplate, String lockKey) {
        this.redisTemplate = redisTemplate;
        this.lockKey = LOCK_PREFIX + lockKey;
        this.lockValue = UUID.randomUUID().toString();
    }

    public boolean tryLock() {
        return tryLock(DEFAULT_TIMEOUT_SECONDS);
    }

    public boolean tryLock(long timeoutSeconds) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(timeoutSeconds));
        return Boolean.TRUE.equals(success);
    }

    public void unlock() {
        String current = redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(current)) {
            redisTemplate.delete(lockKey);
        }
    }
}
