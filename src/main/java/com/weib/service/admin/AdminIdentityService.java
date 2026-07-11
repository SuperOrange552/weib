package com.weib.service.admin;

import com.weib.entity.UserRole;
import com.weib.repository.UserRoleRepository;
import com.weib.session.SessionInvalidationReason;
import com.weib.session.SessionRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminIdentityService {
    private final UserRoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final SessionRegistryService sessionRegistry;

    @Transactional(readOnly = true)
    public List<UserRole> list(Long userId) {
        return roleRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }

    @Transactional
    public UserRole enable(Long adminId, Long userId, String roleType, String reason) {
        String role = normalize(roleType);
        String requiredReason = requireReason(reason);
        UserRole identity = roleRepository.findByUserIdAndRoleType(userId, role).orElseGet(UserRole::new);
        identity.setUserId(userId);
        identity.setRoleType(role);
        identity.setStatus("ACTIVE");
        identity.setEnabledAt(LocalDateTime.now());
        identity.setEnabledBy(adminId);
        UserRole saved = roleRepository.save(identity);
        auditLogService.log(adminId, "enable_identity", "user_role", userId, requiredReason);
        return saved;
    }

    @Transactional
    public UserRole disable(Long adminId, Long userId, String roleType, String reason) {
        String role = normalize(roleType);
        String requiredReason = requireReason(reason);
        UserRole identity = roleRepository.findByUserIdAndRoleType(userId, role)
                .orElseThrow(() -> new IllegalArgumentException("身份不存在"));
        identity.setStatus("DISABLED");
        UserRole saved = roleRepository.save(identity);
        sessionRegistry.invalidateAll(userId, SessionInvalidationReason.ADMIN_FORCED);
        auditLogService.log(adminId, "disable_identity", "user_role", userId, requiredReason);
        return saved;
    }

    private String normalize(String role) {
        String value = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!"SEEKER".equals(value) && !"BOSS".equals(value)) throw new IllegalArgumentException("身份类型无效");
        return value;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("操作原因不能为空");
        return reason.trim();
    }
}
