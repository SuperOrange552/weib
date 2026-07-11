package com.weib.service.admin;

import com.weib.entity.UserRole;
import com.weib.repository.UserRoleRepository;
import com.weib.session.SessionRegistryService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminIdentityServiceTest {
    @Test
    void activatingBossIdentityIsAuditedAndPersisted() {
        UserRoleRepository roles = mock(UserRoleRepository.class);
        AuditLogService audit = mock(AuditLogService.class);
        SessionRegistryService sessions = mock(SessionRegistryService.class);
        when(roles.findByUserIdAndRoleType(7L, "BOSS")).thenReturn(Optional.empty());
        when(roles.save(any())).thenAnswer(i -> i.getArgument(0));
        AdminIdentityService service = new AdminIdentityService(roles, audit, sessions);

        UserRole role = service.enable(99L, 7L, "BOSS", "企业审核通过");

        assertEquals("ACTIVE", role.getStatus());
        assertEquals(99L, role.getEnabledBy());
        verify(audit).log(99L, "enable_identity", "user_role", 7L, "企业审核通过");
    }
}
