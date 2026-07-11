package com.weib.service;

import com.weib.cache.CacheAsideService;
import com.weib.cache.CacheInvalidationService;
import com.weib.cache.CacheKeys;
import com.weib.entity.UserSanction;
import com.weib.exception.SanctionDeniedException;
import com.weib.repository.UserSanctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/** 统一读取和执行用户处罚，避免各个 Controller 自己判断状态。 */
@Service
@RequiredArgsConstructor
public class SanctionService {

    private static final Duration DECISION_TTL = Duration.ofSeconds(15);

    private final UserSanctionRepository repository;
    private final CacheAsideService cache;
    private final CacheInvalidationService cacheInvalidation;

    @Transactional(readOnly = true)
    public boolean hasActive(Long userId, String sanctionType) {
        String normalizedType = normalizeType(sanctionType);
        Boolean active = cache.getOrLoad(CacheKeys.sanction(userId, normalizedType), Boolean.class,
                () -> loadActive(userId, normalizedType), DECISION_TTL);
        return Boolean.TRUE.equals(active);
    }

    public void assertAllowed(Long userId, String sanctionType) {
        if (userId != null && hasActive(userId, sanctionType)) {
            throw new SanctionDeniedException(normalizeType(sanctionType));
        }
    }

    public void invalidate(Long userId, String sanctionType) {
        if (userId != null && sanctionType != null) {
            cacheInvalidation.invalidate(CacheKeys.sanction(userId, normalizeType(sanctionType)));
        }
    }

    private boolean loadActive(Long userId, String type) {
        LocalDateTime now = LocalDateTime.now();
        return !repository.findActivePermanent(userId, type, "ACTIVE", now).isEmpty()
                || !repository.findActiveTemporary(userId, type, "ACTIVE", now).isEmpty();
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("处罚类型不能为空");
        return type.trim().toUpperCase(Locale.ROOT);
    }
}
