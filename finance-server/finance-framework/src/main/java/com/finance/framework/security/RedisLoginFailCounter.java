package com.finance.framework.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 登录失败计数：失败次数 key + 锁定 key，锁定 15 分钟（硬约束 21）。
 *
 * <p>实现语义：5 次失败触发锁定；锁定期间拒绝登录；成功后清除计数。</p>
 */
@Component
public class RedisLoginFailCounter implements LoginFailCounter {

    private static final String FAIL_KEY_PREFIX = "auth:fail:";
    private static final String LOCK_KEY_PREFIX = "auth:lock:";

    private final StringRedisTemplate redisTemplate;
    private final int maxFailTimes;
    private final long lockMinutes;

    public RedisLoginFailCounter(
            StringRedisTemplate redisTemplate,
            @Value("${finance.auth.max-fail:5}") int maxFailTimes,
            @Value("${finance.auth.lock-minutes:15}") long lockMinutes) {
        this.redisTemplate = redisTemplate;
        this.maxFailTimes = maxFailTimes;
        this.lockMinutes = lockMinutes;
    }

    @Override
    public long recordFailure(String username) {
        String failKey = FAIL_KEY_PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1) {
            redisTemplate.expire(failKey, Duration.ofMinutes(lockMinutes));
        }
        if (count != null && count >= maxFailTimes) {
            redisTemplate.opsForValue().set(LOCK_KEY_PREFIX + username, "1", Duration.ofMinutes(lockMinutes));
        }
        return count == null ? 0 : count;
    }

    @Override
    public void clearFailure(String username) {
        redisTemplate.delete(FAIL_KEY_PREFIX + username);
        redisTemplate.delete(LOCK_KEY_PREFIX + username);
    }

    @Override
    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY_PREFIX + username));
    }

    @Override
    public long lockRemainingSeconds(String username) {
        Long ttl = redisTemplate.getExpire(LOCK_KEY_PREFIX + username);
        return ttl == null || ttl < 0 ? 0 : ttl;
    }
}
