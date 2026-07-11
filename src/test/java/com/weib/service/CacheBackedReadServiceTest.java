package com.weib.service;

import com.weib.cache.CacheAsideService;
import com.weib.cache.CacheInvalidationService;
import com.weib.cache.CacheKeys;
import com.weib.dto.PublicUserProfile;
import com.weib.entity.Company;
import com.weib.entity.Job;
import com.weib.entity.Resume;
import com.weib.repository.ApplicationRepository;
import com.weib.repository.CompanyRepository;
import com.weib.repository.JobRepository;
import com.weib.repository.ResumeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CacheBackedReadServiceTest {

    @Test
    void jobDetailReadUsesCacheBeforeRepository() {
        JobRepository jobs = mock(JobRepository.class);
        ApplicationRepository applications = mock(ApplicationRepository.class);
        CacheAsideService cache = mock(CacheAsideService.class);
        CacheInvalidationService invalidation = mock(CacheInvalidationService.class);
        Job cached = new Job();
        cached.setId(1L);
        cached.setTitle("缓存职位");
        when(cache.getOrLoad(eq(CacheKeys.job(1L)), eq(Job.class), any(), any(Duration.class)))
                .thenReturn(cached);

        JobService service = new JobService(jobs, applications, cache, invalidation);

        assertThat(service.getJobById(1L)).isSameAs(cached);
        verifyNoInteractions(jobs);
    }

    @Test
    void companyDetailReadFallsBackToRepositoryThroughCacheLoader() {
        CompanyRepository companies = mock(CompanyRepository.class);
        MapService maps = mock(MapService.class);
        CacheAsideService cache = mock(CacheAsideService.class);
        CacheInvalidationService invalidation = mock(CacheInvalidationService.class);
        Company company = new Company();
        company.setId(2L);
        when(companies.findById(2L)).thenReturn(Optional.of(company));
        when(cache.getOrLoad(eq(CacheKeys.company(2L)), eq(Company.class), any(), any(Duration.class)))
                .thenAnswer(invocation -> invocation.<java.util.function.Supplier<Company>>getArgument(2).get());

        CompanyService service = new CompanyService(companies, maps, cache, invalidation);

        assertThat(service.getCompanyById(2L)).isSameAs(company);
        verify(companies).findById(2L);
    }

    @Test
    void resumeByUserReadUsesUserCacheKey() {
        ResumeRepository resumes = mock(ResumeRepository.class);
        CacheAsideService cache = mock(CacheAsideService.class);
        CacheInvalidationService invalidation = mock(CacheInvalidationService.class);
        Resume resume = new Resume();
        resume.setId(3L);
        resume.setUserId(7L);
        when(cache.getOrLoad(eq(CacheKeys.resumeByUser(7L)), eq(Resume.class), any(), any(Duration.class)))
                .thenReturn(resume);

        ResumeService service = new ResumeService(resumes, cache, invalidation);

        assertThat(service.getResumeByUserId(7L)).isSameAs(resume);
        verifyNoInteractions(resumes);
    }

    @Test
    void publicUserReadUsesSafeUserCacheValue() {
        com.weib.repository.UserRepository users = mock(com.weib.repository.UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        CacheAsideService cache = mock(CacheAsideService.class);
        CacheInvalidationService invalidation = mock(CacheInvalidationService.class);
        PublicUserProfile profile = new PublicUserProfile(9L, "seeker", "求职者", "seeker-name", "/avatar.png", "active");
        when(cache.getOrLoad(eq(CacheKeys.userPublic(9L)), eq(PublicUserProfile.class), any(), any(Duration.class)))
                .thenReturn(profile);

        UserService service = new UserService(users, encoder, cache, invalidation);

        assertThat(service.getPublicUserProfile(9L)).isSameAs(profile);
        verifyNoInteractions(users);
    }
}
