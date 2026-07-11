package com.weib.service;

import com.weib.entity.RoleProfile;
import com.weib.exception.RoleNotEnabledException;
import com.weib.repository.RoleProfileRepository;
import com.weib.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdentityService {
    public static final String SEEKER = "SEEKER";
    public static final String BOSS = "BOSS";
    public static final String ACTIVE = "ACTIVE";

    private final UserRoleRepository userRoleRepository;
    private final RoleProfileRepository roleProfileRepository;

    public Set<String> enabledRoles(Long userId) {
        Set<String> result = new LinkedHashSet<>();
        userRoleRepository.findByUserIdAndStatus(userId, ACTIVE)
                .forEach(role -> result.add(normalizeRole(role.getRoleType())));
        return Set.copyOf(result);
    }

    public String requireEnabledRole(Long userId, String role) {
        String canonical = normalizeRole(role);
        if (!userRoleRepository.existsByUserIdAndRoleTypeAndStatus(userId, canonical, ACTIVE)) {
            throw new RoleNotEnabledException(canonical);
        }
        return canonical;
    }

    public Optional<RoleProfile> profile(Long userId, String role) {
        return roleProfileRepository.findByUserIdAndRoleType(userId, normalizeRole(role));
    }

    public String normalizeRole(String role) {
        if (role == null) throw new IllegalArgumentException("角色不能为空");
        String canonical = role.trim().toUpperCase(Locale.ROOT);
        if (!SEEKER.equals(canonical) && !BOSS.equals(canonical)) {
            throw new IllegalArgumentException("不支持的业务角色: " + role);
        }
        return canonical;
    }
}
