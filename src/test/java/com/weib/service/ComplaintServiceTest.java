package com.weib.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weib.dto.ComplaintCreateRequest;
import com.weib.entity.Company;
import com.weib.entity.Complaint;
import com.weib.entity.Job;
import com.weib.entity.User;
import com.weib.exception.DuplicateComplaintException;
import com.weib.repository.CompanyRepository;
import com.weib.repository.ComplaintRepository;
import com.weib.repository.JobRepository;
import com.weib.repository.ResumeRepository;
import com.weib.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ComplaintServiceTest {

    @Test
    void validJobComplaintIsSavedWithNormalizedFields() {
        ComplaintRepository complaints = mock(ComplaintRepository.class);
        UserRepository users = mock(UserRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        CompanyRepository companies = mock(CompanyRepository.class);
        ResumeRepository resumes = mock(ResumeRepository.class);
        User reporter = new User();
        reporter.setId(1L);
        when(users.existsById(1L)).thenReturn(true);
        Job job = new Job();
        job.setId(20L);
        job.setCompanyId(30L);
        Company company = new Company();
        company.setId(30L);
        company.setBossId(2L);
        when(jobs.findById(20L)).thenReturn(Optional.of(job));
        when(companies.findById(30L)).thenReturn(Optional.of(company));
        when(complaints.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(1L, "JOB", 20L, "PENDING"))
                .thenReturn(false);
        when(complaints.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        ComplaintService service = new ComplaintService(complaints, users, jobs, companies, resumes, new ObjectMapper());

        var response = service.create(1L, new ComplaintCreateRequest(
                "job", 20L, "fake_job", "  薪资与实际不符  ", List.of("/uploads/evidence.png")));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo("PENDING");
        verify(complaints).save(argThat(c -> c.getTargetType().equals("JOB")
                && c.getCategory().equals("FAKE_JOB")
                && c.getDescription().equals("薪资与实际不符")));
    }

    @Test
    void duplicatePendingComplaintIsRejected() {
        ComplaintRepository complaints = mock(ComplaintRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(users.existsById(1L)).thenReturn(true);
        when(users.existsById(2L)).thenReturn(true);
        when(complaints.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(1L, "USER", 2L, "PENDING"))
                .thenReturn(true);
        ComplaintService service = new ComplaintService(complaints, users, mock(JobRepository.class),
                mock(CompanyRepository.class), mock(ResumeRepository.class), new ObjectMapper());

        assertThatThrownBy(() -> service.create(1L,
                new ComplaintCreateRequest("USER", 2L, "FRAUD", "诈骗账号", List.of())))
                .isInstanceOf(DuplicateComplaintException.class);
    }

    @Test
    void userCannotReportSelf() {
        ComplaintService service = new ComplaintService(mock(ComplaintRepository.class), mock(UserRepository.class),
                mock(JobRepository.class), mock(CompanyRepository.class), mock(ResumeRepository.class), new ObjectMapper());

        assertThatThrownBy(() -> service.create(1L,
                new ComplaintCreateRequest("USER", 1L, "FRAUD", "自我投诉", List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
