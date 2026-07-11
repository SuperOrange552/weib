package com.weib.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 统一生成 Redis 业务 Key，避免不同服务使用不一致的命名。 */
public final class CacheKeys {

    private CacheKeys() {
    }

    public static String userPublic(Long userId) {
        return "cache:user:public:" + userId;
    }

    public static String company(Long companyId) {
        return "cache:company:" + companyId;
    }

    public static String job(Long jobId) {
        return "cache:job:" + jobId;
    }

    public static String jobsList(String normalizedQuery, int page, int size) {
        return "cache:jobs:list:" + sha256(normalizedQuery) + ":" + page + ":" + size;
    }

    public static String resumePublic(Long resumeId) {
        return "cache:resume:public:" + resumeId;
    }

    public static String resumeByUser(Long userId) {
        return "cache:resume:user:" + userId;
    }

    public static String loadLock(String cacheKey) {
        return "lock:cache:load:" + cacheKey;
    }

    public static String sanction(Long userId, String sanctionType) {
        return "cache:sanction:" + userId + ":" + sanctionType;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
