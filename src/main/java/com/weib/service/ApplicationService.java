package com.weib.service;

import com.weib.entity.Application;
import com.weib.entity.Job;
import com.weib.entity.Resume;
import com.weib.repository.ApplicationRepository;
import com.weib.repository.JobRepository;
import com.weib.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ============================================
 * 【Service】投递记录业务逻辑层
 * ============================================
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;

    /**
     * 投递职位
     * 检查：1) 是否已投递  2) 简历是否存在
     */
    @Transactional
    public Application apply(Long jobId, Long userId) {
        // 检查是否已投递
        if (hasApplied(jobId, userId)) {
            throw new RuntimeException("您已投递过该职位，请勿重复投递");
        }

        // 检查简历是否存在
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("请先完善简历后再投递"));

        // 检查职位是否存在且活跃
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("职位不存在: " + jobId));
        if (!"active".equals(job.getStatus())) {
            throw new RuntimeException("该职位已关闭，无法投递");
        }

        Application application = new Application();
        application.setJobId(jobId);
        application.setUserId(userId);
        application.setResumeId(resume.getId());
        return applicationRepository.save(application);
    }

    /**
     * 获取用户的投递记录
     */
    @Transactional(readOnly = true)
    public List<Application> getApplicationsByUser(Long userId) {
        return applicationRepository.findByUserId(userId);
    }

    /**
     * 获取职位的投递记录
     */
    @Transactional(readOnly = true)
    public List<Application> getApplicationsByJob(Long jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    /**
     * 批量获取多个职位的投递记录（避免 N+1 查询）
     */
    @Transactional(readOnly = true)
    public List<Application> getApplicationsByJobIds(List<Long> jobIds) {
        if (jobIds.isEmpty()) return List.of();
        return applicationRepository.findByJobIdIn(jobIds);
    }

    /**
     * 获取公司的投递记录
     * 通过职位ID间接查询：先查公司所有职位，再查这些职位的投递
     */
    @Transactional(readOnly = true)
    public List<Application> getApplicationsByCompany(Long companyId) {
        List<Job> jobs = jobRepository.findByCompanyId(companyId);
        if (jobs.isEmpty()) {
            return List.of();
        }
        List<Long> jobIds = jobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
        return applicationRepository.findByJobIdIn(jobIds);
    }

    /**
     * 根据ID获取投递记录（用于权限校验）
     */
    @Transactional(readOnly = true)
    public Application getApplicationById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("投递记录不存在: " + id));
    }

    /**
     * 更新投递状态（Boss操作）
     */
    @Transactional
    public void updateStatus(Long applicationId, String status, String bossNote) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("投递记录不存在: " + applicationId));
        application.setStatus(status);
        if (bossNote != null) {
            application.setBossNote(bossNote);
        }
        applicationRepository.save(application);
    }

    /**
     * 用户是否已投递该职位
     */
    @Transactional(readOnly = true)
    public boolean hasApplied(Long jobId, Long userId) {
        return applicationRepository.findByJobIdAndUserId(jobId, userId).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean hasAppliedToAny(List<Long> jobIds, Long userId) {
        if (jobIds.isEmpty()) return false;
        return !applicationRepository.findByJobIdInAndUserId(jobIds, userId).isEmpty();
    }
}