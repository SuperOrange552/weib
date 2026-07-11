package com.weib.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        this.redis = redis;
        this.scheduler = scheduler;
        this.enabled = true;
        this.delayedDeleteMillis = Math.max(0, delayedDeleteMillis);
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

    /** 使用 Redis SCAN 清理命名空间，避免生产环境调用阻塞性的 KEYS。 */
    public void invalidatePattern(String pattern) {
        if (!enabled || pattern == null || pattern.isBlank()) return;
        try {
            Set<String> keys = redis.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> found = new HashSet<>();
                try (Cursor<byte[]> cursor = connection.scan(
                        ScanOptions.scanOptions().match(pattern).count(100).build())) {
                    while (cursor.hasNext()) {
                        found.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return found;
            });
            if (keys != null && !keys.isEmpty()) invalidate(keys.toArray(new String[0]));
        } catch (RuntimeException e) {
            log.warn("Redis SCAN invalidation failed, pattern={}, error={}", pattern, e.getMessage());
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
