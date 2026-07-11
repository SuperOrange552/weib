package com.weib.service;

import com.weib.entity.UserRole;
import com.weib.exception.RoleNotEnabledException;
import com.weib.repository.RoleProfileRepository;
import com.weib.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityServiceTest {
    private UserRoleRepository roleRepository;
    private IdentityService service;

    @BeforeEach
    void setUp() {
        roleRepository = mock(UserRoleRepository.class);
        service = new IdentityService(roleRepository, mock(RoleProfileRepository.class));
    }

    @Test
    void returnsAllEnabledRolesInCanonicalForm() {
        when(roleRepository.findByUserIdAndStatus(7L, "ACTIVE")).thenReturn(List.of(
                role(7L, "seeker", "ACTIVE"), role(7L, "BOSS", "ACTIVE")));

        assertEquals(Set.of("SEEKER", "BOSS"), service.enabledRoles(7L));
    }

    @Test
    void requiresRoleToBeEnabledForTheAccount() {
        when(roleRepository.existsByUserIdAndRoleTypeAndStatus(8L, "BOSS", "ACTIVE")).thenReturn(false);

        RoleNotEnabledException error = assertThrows(RoleNotEnabledException.class,
                () -> service.requireEnabledRole(8L, "boss"));
        assertEquals("ROLE_NOT_ENABLED", error.getReason());
    }

    @Test
    void rejectsUnknownBusinessRoleBeforeRepositoryLookup() {
        assertThrows(IllegalArgumentException.class,
                () -> service.requireEnabledRole(8L, "ADMIN"));
    }

    private UserRole role(Long userId, String type, String status) {
        UserRole role = new UserRole();
        role.setUserId(userId);
        role.setRoleType(type);
        role.setStatus(status);
        return role;
    }
}
