package com.weib.service;

import com.weib.cache.CacheAsideService;
import com.weib.cache.CacheInvalidationService;
import com.weib.cache.CacheKeys;
import com.weib.entity.UserSanction;
import com.weib.exception.SanctionDeniedException;
import com.weib.repository.UserSanctionRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SanctionServiceTest {

    @Test
    void activeTemporarySanctionBlocksAndUsesShortCache() {
        UserSanctionRepository repository = mock(UserSanctionRepository.class);
        CacheAsideService cache = mock(CacheAsideService.class);
        CacheInvalidationService invalidation = mock(CacheInvalidationService.class);
        UserSanction sanction = new UserSanction();
        sanction.setEndsAt(LocalDateTime.now().plusMinutes(10));
        when(repository.findActivePermanent(anyLong(), anyString(), anyString(), any())).thenReturn(List.of());
        when(repository.findActiveTemporary(anyLong(), anyString(), anyString(), any())).thenReturn(List.of(sanction));
        when(cache.getOrLoad(eq(CacheKeys.sanction(8L, "MUTE")), eq(Boolean.class), any(), any(Duration.class)))
                .thenAnswer(invocation -> invocation.<java.util.function.Supplier<Boolean>>getArgument(2).get());

        SanctionService service = new SanctionService(repository, cache, invalidation);

        assertThat(service.hasActive(8L, "MUTE")).isTrue();
        assertThatThrownBy(() -> service.assertAllowed(8L, "MUTE"))
                .isInstanceOf(SanctionDeniedException.class);
    }

    @Test
    void expiredSanctionDoesNotBlock() {
        UserSanctionRepository repository = mock(UserSanctionRepository.class);
        CacheAsideService cache = mock(CacheAsideService.class);
        CacheInvalidationService invalidation = mock(CacheInvalidationService.class);
        when(repository.findActivePermanent(anyLong(), anyString(), anyString(), any())).thenReturn(List.of());
        when(repository.findActiveTemporary(anyLong(), anyString(), anyString(), any())).thenReturn(List.of());
        when(cache.getOrLoad(eq(CacheKeys.sanction(9L, "PUBLISH_BAN")), eq(Boolean.class), any(), any(Duration.class)))
                .thenAnswer(invocation -> invocation.<java.util.function.Supplier<Boolean>>getArgument(2).get());

        SanctionService service = new SanctionService(repository, cache, invalidation);

        assertThat(service.hasActive(9L, "PUBLISH_BAN")).isFalse();
        service.assertAllowed(9L, "PUBLISH_BAN");
    }
}
