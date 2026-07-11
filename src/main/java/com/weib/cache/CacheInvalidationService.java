package com.weib.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;

/** 数据库事务成功后立即删除并延迟二次删除缓存。 */
@Slf4j
@Service
public class CacheInvalidationService {

    private final RedisTemplate<String, Object> redis;
    private final TaskScheduler scheduler;
    private final boolean enabled;
    private final long delayedDeleteMillis;

    @Autowired
    public CacheInvalidationService(RedisTemplate<String, Object> redis,
                                    TaskScheduler scheduler,
                                    @Value("${weib.cache.enabled:true}") boolean enabled,
                                    @Value("${weib.cache.delayed-delete-ms:800}") long delayedDeleteMillis) {
        this.redis = redis;
        this.scheduler = scheduler;
        this.enabled = enabled;
        this.delayedDeleteMillis = Math.max(0, delayedDeleteMillis);
    }

    public CacheInvalidationService(RedisTemplate<String, Object> redis,
                                    TaskScheduler scheduler,
                                    long delayedDeleteMillis) {
        this(redis, scheduler, true, delayedDeleteMillis);
    }

    public void invalidate(String... keys) {
        if (!enabled) return;
        String[] normalized = Arrays.stream(keys)
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toArray(String[]::new);
        for (String key : normalized) deleteQuietly(key);
        if (normalized.length == 0) return;

        try {
            scheduler.schedule(() -> {
                for (String key : normalized) deleteQuietly(key);
            }, Instant.now().plusMillis(delayedDeleteMillis));
        } catch (RuntimeException e) {
            log.warn("Unable to schedule delayed cache deletion, keys={}, error={}",
                    Arrays.toString(normalized), e.getMessage());
        }
    }

    private void deleteQuietly(String key) {
        try {
            redis.delete(key);
        } catch (RuntimeException e) {
            log.warn("Redis cache deletion failed, key={}, error={}", key, e.getMessage());
        }
    }
}
