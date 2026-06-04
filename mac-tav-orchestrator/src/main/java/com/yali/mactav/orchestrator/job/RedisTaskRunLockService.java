package com.yali.mactav.orchestrator.job;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis SETNX task lock with token-checked release.
 */
public class RedisTaskRunLockService implements TaskRunLockService {

    private static final String PREFIX = "mactav:task:lock:";

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisTaskRunLockService(StringRedisTemplate redisTemplate, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    @Override
    public Optional<TaskRunLock> tryLock(String taskId, String token) {
        String key = PREFIX + taskId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(acquired) ? Optional.of(new TaskRunLock(taskId, key, token)) : Optional.empty();
    }

    @Override
    public boolean isLocked(String taskId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + taskId));
    }

    @Override
    public void unlock(TaskRunLock lock) {
        if (lock == null) {
            return;
        }
        redisTemplate.execute(RELEASE_SCRIPT, List.of(lock.lockKey()), lock.token());
    }
}
