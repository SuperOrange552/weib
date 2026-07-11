package com.weib.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CacheInvalidationServiceTest {

    private RedisTemplate<String, Object> redis;
    private TaskScheduler scheduler;
    private CacheInvalidationService service;

    @BeforeEach
    void setUp() {
        redis = mock(RedisTemplate.class);
        scheduler = mock(TaskScheduler.class);
        service = new CacheInvalidationService(redis, scheduler, 800);
    }

    @Test
    void invalidationDeletesImmediatelyAndSchedulesDelayedDelete() {
        service.invalidate("cache:job:1", "cache:jobs:list:all:0:20");

        verify(redis).delete("cache:job:1");
        verify(redis).delete("cache:jobs:list:all:0:20");
        verify(scheduler).schedule(any(Runnable.class), any(Instant.class));
    }
}
