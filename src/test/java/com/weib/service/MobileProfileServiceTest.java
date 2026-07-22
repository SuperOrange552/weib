package com.weib.service;

import com.weib.cache.CacheInvalidationService;
import com.weib.entity.RoleProfile;
import com.weib.repository.RoleProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobileProfileServiceTest {
    @Test
    void updatesActiveRoleNicknameAndInvalidatesForumCache() {
        IdentityService identities = mock(IdentityService.class);
        RoleProfileRepository profiles = mock(RoleProfileRepository.class);
        CacheInvalidationService invalidation = mock(CacheInvalidationService.class);
        when(identities.requireEnabledRole(7L, "seeker")).thenReturn("SEEKER");
        when(profiles.findByUserIdAndRoleType(7L, "SEEKER")).thenReturn(Optional.empty());
        when(profiles.save(any(RoleProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MobileProfileService service = new MobileProfileService(identities, profiles, invalidation);
        RoleProfile saved = service.updateNickname(7L, "seeker", " 自动化昵称 ");

        assertEquals("SEEKER", saved.getRoleType());
        assertEquals("自动化昵称", saved.getNickname());
        verify(invalidation).invalidatePattern("cache:forum:*");
    }

    @Test
    void rejectsBlankNickname() {
        MobileProfileService service = new MobileProfileService(
                mock(IdentityService.class), mock(RoleProfileRepository.class),
                mock(CacheInvalidationService.class));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateNickname(7L, "SEEKER", " "));
    }
}

