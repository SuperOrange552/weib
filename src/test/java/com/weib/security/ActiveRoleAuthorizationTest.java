package com.weib.security;

import com.weib.entity.RoleProfile;
import com.weib.entity.User;
import com.weib.identity.ActiveIdentity;
import com.weib.identity.ActiveIdentityResolver;
import com.weib.service.IdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActiveRoleAuthorizationTest {
    @Test
    void authorizationUsesSelectedIdentityInsteadOfLegacyUserRole() {
        IdentityService identities = mock(IdentityService.class);
        ActiveIdentityResolver resolver = new ActiveIdentityResolver(identities);
        User dualRole = new User();
        dualRole.setId(7L);
        dualRole.setRole("seeker");
        dualRole.setNickname("旧昵称");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", dualRole);
        session.setAttribute("activeRole", "BOSS");
        RoleProfile bossProfile = new RoleProfile();
        bossProfile.setNickname("招聘者阿华");
        when(identities.requireEnabledRole(7L, "BOSS")).thenReturn("BOSS");
        when(identities.profile(7L, "BOSS")).thenReturn(Optional.of(bossProfile));

        ActiveIdentity boss = resolver.require(session, "BOSS");

        assertEquals("BOSS", boss.role());
        assertEquals("招聘者阿华", boss.nickname());
        assertEquals("BOSS", resolver.current(session).role());
        assertThrows(AccessDeniedException.class, () -> resolver.require(session, "SEEKER"));
    }
}
