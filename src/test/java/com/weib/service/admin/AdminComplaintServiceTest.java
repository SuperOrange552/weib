package com.weib.service.admin;

import com.weib.cache.CacheInvalidationService;
import com.weib.dto.admin.ComplaintReviewRequest;
import com.weib.dto.admin.SanctionCreateRequest;
import com.weib.entity.Complaint;
import com.weib.entity.UserSanction;
import com.weib.repository.*;
import com.weib.service.NotificationService;
import com.weib.service.SanctionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminComplaintServiceTest {

    @Test
    void resolveComplaintCreatesSanctionAuditAndNotification() {
        ComplaintRepository complaints = mock(ComplaintRepository.class);
        UserSanctionRepository sanctions = mock(UserSanctionRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        CompanyRepository companies = mock(CompanyRepository.class);
        ResumeRepository resumes = mock(ResumeRepository.class);
        UserRepository users = mock(UserRepository.class);
        AuditLogService audit = mock(AuditLogService.class);
        NotificationService notifications = mock(NotificationService.class);
        SanctionService sanctionService = mock(SanctionService.class);
        CacheInvalidationService invalidation = mock(CacheInvalidationService.class);
        Complaint complaint = new Complaint();
        complaint.setId(1L);
        complaint.setReporterId(10L);
        complaint.setTargetType("USER");
        complaint.setTargetId(20L);
        complaint.setStatus("PENDING");
        when(complaints.findById(1L)).thenReturn(Optional.of(complaint));
        when(users.existsById(20L)).thenReturn(true);
        when(complaints.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sanctions.save(any(UserSanction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminComplaintService service = new AdminComplaintService(complaints, sanctions, jobs, companies, resumes,
                users, audit, notifications, sanctionService, invalidation);

        service.resolve(99L, 1L, new ComplaintReviewRequest("确认违规", null,
                new SanctionCreateRequest(20L, "MUTE", "USER", 20L, 1L,
                        "投诉属实", LocalDateTime.now(), LocalDateTime.now().plusDays(7))));

        assertThat(complaint.getStatus()).isEqualTo("RESOLVED");
        verify(sanctions).save(argThat(s -> s.getUserId().equals(20L)
                && s.getSanctionType().equals("MUTE")
                && s.getEndsAt() != null));
        verify(audit).log(eq(99L), eq("resolve_complaint"), eq("complaint"), eq(1L), anyString());
        verify(notifications).createSystemNotification(eq(10L), eq("complaint_resolved"), anyString(), eq(1L));
    }

    @Test
    void rejectComplaintOnlyChangesPendingComplaint() {
        ComplaintRepository complaints = mock(ComplaintRepository.class);
        Complaint complaint = new Complaint();
        complaint.setId(2L);
        complaint.setReporterId(10L);
        complaint.setStatus("PENDING");
        when(complaints.findById(2L)).thenReturn(Optional.of(complaint));
        when(complaints.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AdminComplaintService service = new AdminComplaintService(complaints, mock(UserSanctionRepository.class),
                mock(JobRepository.class), mock(CompanyRepository.class), mock(ResumeRepository.class),
                mock(UserRepository.class), mock(AuditLogService.class), mock(NotificationService.class),
                mock(SanctionService.class), mock(CacheInvalidationService.class));

        service.reject(99L, 2L, "证据不足");

        assertThat(complaint.getStatus()).isEqualTo("REJECTED");
        verify(complaints).save(complaint);
    }
}
