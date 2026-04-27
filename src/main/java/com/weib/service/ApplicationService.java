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

        // 检查职位是否存在
        jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("职位不存在: " + jobId));

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
     * 获取公司的投递记录
     * 通过职位ID间接查询：先查公司所有职位，再查这些职位的投递
     */
    @Transactional(readOnly = true)
    public List<Application> getApplicationsByCompany(Long companyId) {
        // 查询该公司所有职位
        List<Job> jobs = jobRepository.findByCompanyId(companyId);
        if (jobs.isEmpty()) {
            return List.of();
        }
        // 收集所有职位的投递
        java.util.ArrayList<Application> allApps = new java.util.ArrayList<>();
        for (Job job : jobs) {
            List<Application> apps = applicationRepository.findByJobId(job.getId());
            allApps.addAll(apps);
        }
        return allApps;
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
}