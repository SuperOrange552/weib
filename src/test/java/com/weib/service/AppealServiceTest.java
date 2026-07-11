package com.weib.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weib.entity.SanctionAppeal;
import com.weib.entity.UserSanction;
import com.weib.repository.SanctionAppealRepository;
import com.weib.repository.UserRepository;
import com.weib.repository.UserSanctionRepository;
import com.weib.service.admin.AuditLogService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppealServiceTest {
    @Test
    void createsPendingAppealForUsersOwnActiveSanction() {
        SanctionAppealRepository appeals = mock(SanctionAppealRepository.class);
        UserSanctionRepository sanctions = mock(UserSanctionRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(users.existsById(5L)).thenReturn(true);
        when(sanctions.findById(9L)).thenReturn(Optional.of(activeSanction(9L, 5L)));
        when(appeals.findFirstBySanctionIdAndUserIdAndStatus(9L, 5L, "PENDING"))
                .thenReturn(Optional.empty());
        when(appeals.save(any(SanctionAppeal.class))).thenAnswer(invocation -> {
            SanctionAppeal saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        AppealService service = service(appeals, sanctions, users);
        var result = service.create(5L, new com.weib.dto.AppealCreateRequest(
                9L, "请复核处罚事实", java.util.List.of("/uploads/appeals/proof.png")));

        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.status()).isEqualTo("PENDING");
        verify(appeals).save(argThat(a -> a.getUserId().equals(5L)
                && a.getSanctionId().equals(9L)
                && a.getEvidenceUrls().contains("proof.png")));
    }

    @Test
    void duplicatePendingAppealIsRejected() {
        SanctionAppealRepository appeals = mock(SanctionAppealRepository.class);
        UserSanctionRepository sanctions = mock(UserSanctionRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(users.existsById(5L)).thenReturn(true);
        when(sanctions.findById(9L)).thenReturn(Optional.of(activeSanction(9L, 5L)));
        when(appeals.findFirstBySanctionIdAndUserIdAndStatus(9L, 5L, "PENDING"))
                .thenReturn(Optional.of(new SanctionAppeal()));

        AppealService service = service(appeals, sanctions, users);
        assertThatThrownBy(() -> service.create(5L,
                new com.weib.dto.AppealCreateRequest(9L, "重复申诉", java.util.List.of())))
                .isInstanceOf(com.weib.exception.DuplicateAppealException.class);
    }

    private AppealService service(SanctionAppealRepository appeals, UserSanctionRepository sanctions,
                                  UserRepository users) {
        return new AppealService(appeals, sanctions, users, new ObjectMapper(),
                mock(SanctionService.class), mock(com.weib.cache.CacheInvalidationService.class),
                mock(AuditLogService.class), mock(com.weib.service.NotificationService.class));
    }

    private UserSanction activeSanction(Long id, Long userId) {
        UserSanction sanction = new UserSanction();
        sanction.setId(id);
        sanction.setUserId(userId);
        sanction.setSanctionType("ACCOUNT_BAN");
        sanction.setStatus("ACTIVE");
        sanction.setStartsAt(LocalDateTime.now().minusMinutes(1));
        sanction.setReason("test");
        sanction.setAdminId(1L);
        return sanction;
    }
}