package com.weib.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CacheAsideServiceTest {

    private RedisTemplate<String, Object> redis;
    private ValueOperations<String, Object> values;
    private CacheAsideService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(RedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        service = new CacheAsideService(redis, 0, 0, 0);
    }

    @Test
    void cacheHitDoesNotCallLoader() {
        when(values.get("cache:job:1")).thenReturn("cached-job");

        String result = service.getOrLoad("cache:job:1", String.class,
                () -> "database-job", Duration.ofMinutes(5));

        assertThat(result).isEqualTo("cached-job");
        verify(values, never()).set(anyString(), any(), any());
    }

    @Test
    void cacheMissUsesLockLoadsAndWritesValue() {
        when(values.get("cache:job:2")).thenReturn(null);
        when(values.setIfAbsent(anyString(), any(), any())).thenReturn(true);

        String result = service.getOrLoad("cache:job:2", String.class,
                () -> "database-job", Duration.ofMinutes(5));

        assertThat(result).isEqualTo("database-job");
        verify(values).set("cache:job:2", "database-job", Duration.ofMinutes(5));
        verify(redis).delete(anyString());
    }

    @Test
    void nullResultUsesShortLivedNegativeCache() {
        when(values.get("cache:job:404")).thenReturn(null);
        when(values.setIfAbsent(anyString(), any(), any())).thenReturn(true);

        String result = service.getOrLoad("cache:job:404", String.class,
                () -> null, Duration.ofMinutes(5));

        assertThat(result).isNull();
        verify(values).set(eq("cache:job:404"), eq(CacheAsideService.NULL_MARKER), eq(Duration.ofSeconds(30)));
    }

    @Test
    void redisFailureFallsBackToLoader() {
        when(values.get("cache:job:3")).thenThrow(new IllegalStateException("redis down"));

        String result = service.getOrLoad("cache:job:3", String.class,
                () -> "database-job", Duration.ofMinutes(5));

        assertThat(result).isEqualTo("database-job");
    }

    @Test
    void sourceFailureIsNotExecutedTwice() {
        when(values.get("cache:job:5")).thenReturn(null);
        when(values.setIfAbsent(anyString(), any(), any())).thenReturn(true);
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> service.getOrLoad("cache:job:5", String.class, () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("database down");
        }, Duration.ofMinutes(5))).isInstanceOf(IllegalStateException.class);

        assertThat(calls).hasValue(1);
    }
}
