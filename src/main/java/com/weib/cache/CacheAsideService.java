package com.weib.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** Redis 优先、数据库回源的 Cache-Aside 读取服务。 */
@Slf4j
@Service
public class CacheAsideService {

    public static final String NULL_MARKER = "__WEIB_CACHE_NULL__";
    private static final Duration NULL_TTL = Duration.ofSeconds(30);
    private static final Duration LOCK_TTL = Duration.ofSeconds(8);

    private final RedisTemplate<String, Object> redis;
    private final boolean enabled;
    private final long lockWaitMillis;
    private final long lockPollMillis;
    private final int jitterSeconds;

    @Autowired
    public CacheAsideService(RedisTemplate<String, Object> redis,
                             @Value("${weib.cache.enabled:true}") boolean enabled,
                             @Value("${weib.cache.lock-wait-ms:150}") long lockWaitMillis,
                             @Value("${weib.cache.lock-poll-ms:10}") long lockPollMillis,
                             @Value("${weib.cache.ttl-jitter-seconds:30}") int jitterSeconds) {
        this.redis = redis;
        this.enabled = enabled;
        this.lockWaitMillis = Math.max(0, lockWaitMillis);
        this.lockPollMillis = Math.max(1, lockPollMillis);
        this.jitterSeconds = Math.max(0, jitterSeconds);
    }

    public CacheAsideService(RedisTemplate<String, Object> redis,
                             long lockWaitMillis,
                             long lockPollMillis,
                             int jitterSeconds) {
        this(redis, true, lockWaitMillis, lockPollMillis, jitterSeconds);
    }

    public <T> T getOrLoad(String cacheKey, Class<T> type, Supplier<T> loader, Duration ttl) {
        if (!enabled) return loader.get();
        ValueOperations<String, Object> values;
        boolean sourceLoadStarted = false;
        try {
            values = redis.opsForValue();
            Object cached = values.get(cacheKey);
            if (cached != null) return cast(cached, type);

            String lockKey = CacheKeys.loadLock(cacheKey);
            Boolean acquired = values.setIfAbsent(lockKey, UUID.randomUUID().toString(), LOCK_TTL);
            if (Boolean.TRUE.equals(acquired)) {
                try {
                    Object secondRead = values.get(cacheKey);
                    if (secondRead != null) return cast(secondRead, type);
                    sourceLoadStarted = true;
                    return loadAndCache(cacheKey, loader, ttl, values);
                } finally {
                    redis.delete(lockKey);
                }
            }

            long deadline = System.currentTimeMillis() + lockWaitMillis;
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(lockPollMillis);
                Object retry = values.get(cacheKey);
                if (retry != null) return cast(retry, type);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Cache load wait interrupted, key={}", cacheKey);
        } catch (RuntimeException e) {
            if (sourceLoadStarted) throw e;
            log.warn("Redis cache read failed, falling back to source, key={}, error={}",
                    cacheKey, e.getMessage());
        }

        return loader.get();
    }

    private <T> T loadAndCache(String cacheKey, Supplier<T> loader, Duration ttl,
                               ValueOperations<String, Object> values) {
        T loaded = loader.get();
        try {
            if (loaded == null) {
                values.set(cacheKey, NULL_MARKER, NULL_TTL);
            } else {
                values.set(cacheKey, loaded, withJitter(ttl));
            }
        } catch (RuntimeException e) {
            log.warn("Redis cache write failed, key={}, error={}", cacheKey, e.getMessage());
        }
        return loaded;
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value, Class<T> type) {
        if (NULL_MARKER.equals(value)) return null;
        return type.cast(value);
    }

    private Duration withJitter(Duration ttl) {
        if (jitterSeconds == 0) return ttl;
        return ttl.plusSeconds(ThreadLocalRandom.current().nextInt(jitterSeconds + 1));
    }
}
