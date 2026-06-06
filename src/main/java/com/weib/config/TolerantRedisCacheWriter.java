package com.weib.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Redis 缓存写入器容错包装
 *
 * 所有 Redis 操作失败时静默降级，不抛异常。
 * 缓存未命中或不可用时，业务方法正常执行（查库）。
 */
public class TolerantRedisCacheWriter implements RedisCacheWriter {

    private static final Logger log = LoggerFactory.getLogger(TolerantRedisCacheWriter.class);
    private final RedisCacheWriter delegate;
    private boolean warned = false;

    public TolerantRedisCacheWriter(RedisCacheWriter delegate) {
        this.delegate = delegate;
    }

    private void warnOnce(Throwable e) {
        if (!warned) {
            warned = true;
            log.warn("Redis 不可用，缓存已降级为直查数据库: {}", e.getMessage());
        }
    }

    // ===== RedisCacheWriter abstract methods =====

    @Override
    public byte[] get(String name, byte[] key) {
        try { return delegate.get(name, key); }
        catch (RuntimeException e) { warnOnce(e); return null; }
    }

    @Override
    public void put(String name, byte[] key, byte[] value, Duration ttl) {
        try { delegate.put(name, key, value, ttl); }
        catch (RuntimeException e) { warnOnce(e); }
    }

    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {
        try { return delegate.putIfAbsent(name, key, value, ttl); }
        catch (RuntimeException e) { warnOnce(e); return null; }
    }

    @Override
    public void remove(String name, byte[] key) {
        try { delegate.remove(name, key); }
        catch (RuntimeException e) { warnOnce(e); }
    }

    @Override
    public void clean(String name, byte[] pattern) {
        try { delegate.clean(name, pattern); }
        catch (RuntimeException e) { warnOnce(e); }
    }

    @Override
    public CompletableFuture<Void> store(String name, byte[] key, byte[] value, Duration ttl) {
        try { return delegate.store(name, key, value, ttl); }
        catch (RuntimeException e) { warnOnce(e); return CompletableFuture.completedFuture(null); }
    }

    @Override
    public CompletableFuture<byte[]> retrieve(String name, byte[] key, Duration ttl) {
        try { return delegate.retrieve(name, key, ttl); }
        catch (RuntimeException e) { warnOnce(e); return CompletableFuture.completedFuture(null); }
    }

    @Override
    public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector collector) {
        return new TolerantRedisCacheWriter(delegate.withStatisticsCollector(collector));
    }

    @Override
    public void clearStatistics(String name) {
        try { delegate.clearStatistics(name); }
        catch (RuntimeException e) { warnOnce(e); }
    }

    // ===== CacheStatisticsProvider abstract method =====

    @Override
    public CacheStatistics getCacheStatistics(String name) {
        try { return delegate.getCacheStatistics(name); }
        catch (RuntimeException e) { warnOnce(e); return null; }
    }
}
