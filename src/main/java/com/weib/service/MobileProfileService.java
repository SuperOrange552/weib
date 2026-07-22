package com.weib.service;

import com.weib.cache.CacheInvalidationService;
import com.weib.entity.RoleProfile;
import com.weib.repository.RoleProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 维护移动端当前业务身份的公开资料。 */
@Service
@RequiredArgsConstructor
public class MobileProfileService {
    private final IdentityService identityService;
    private final RoleProfileRepository roleProfileRepository;
    private final CacheInvalidationService cacheInvalidation;

    /**
     * 修改当前已启用业务身份的昵称。
     *
     * 双身份账号分别维护SEEKER和BOSS昵称，避免切换身份后互相覆盖。
     */
    @Transactional
    public RoleProfile updateNickname(Long userId, String role, String rawNickname) {
        String canonicalRole = identityService.requireEnabledRole(userId, role);
        String nickname = normalizeNickname(rawNickname);

        RoleProfile profile = roleProfileRepository.findByUserIdAndRoleType(userId, canonicalRole)
                .orElseGet(() -> {
                    RoleProfile created = new RoleProfile();
                    created.setUserId(userId);
                    created.setRoleType(canonicalRole);
                    return created;
                });
        profile.setNickname(nickname);
        RoleProfile saved = roleProfileRepository.save(profile);

        // 论坛缓存响应中包含作者昵称。昵称变化后清理论坛缓存，避免读到旧名称。
        cacheInvalidation.invalidatePattern("cache:forum:*");
        return saved;
    }

    private String normalizeNickname(String rawNickname) {
        if (rawNickname == null) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        String nickname = rawNickname.trim();
        if (nickname.length() < 2 || nickname.length() > 50) {
            throw new IllegalArgumentException("昵称长度必须为2-50个字符");
        }
        if (nickname.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("昵称不能包含控制字符");
        }
        return nickname;
    }
}

