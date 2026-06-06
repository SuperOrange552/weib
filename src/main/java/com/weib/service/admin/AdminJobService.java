package com.weib.service.admin;

import com.weib.dto.admin.JobListResponse;
import com.weib.entity.Company;
import com.weib.entity.Job;
import com.weib.repository.CompanyRepository;
import com.weib.repository.JobRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 职位审核业务服务
 *
 * 提供职位审核列表查询、详情查看、审核通过/驳回、批量下架等业务操作。
 * 每个审核操作均在同一事务内完成：更新状态 + 记录日志 + 发送通知。
 */
@Service
public class AdminJobService {
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private final com.weib.service.NotificationService notificationService;

    public AdminJobService(JobRepository jobRepository, CompanyRepository companyRepository,
                           AuditLogService auditLogService,
                           com.weib.service.NotificationService notificationService) {
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * 分页查询职位列表（支持审核状态筛选 + 关键词搜索）
     *
     * @param auditStatus 审核状态（pending/approved/rejected），默认 pending
     * @param keyword     搜索关键词（模糊匹配职位标题）
     * @param pageable    分页参数
     * @return 分页职位列表（含公司名称）
     */
    @Transactional(readOnly = true)
    public Page<JobListResponse> listJobs(String auditStatus, String keyword, Pageable pageable) {
        Page<Job> page;
        if (keyword != null && !keyword.isBlank()) {
            if (auditStatus != null && !auditStatus.isBlank()) {
                page = jobRepository.findByTitleContainingIgnoreCaseAndAuditStatus(keyword, auditStatus, pageable);
            } else {
                page = jobRepository.findByTitleContainingIgnoreCase(keyword, pageable);
            }
        } else if (auditStatus != null && !auditStatus.isBlank()) {
            page = jobRepository.findByAuditStatusOrderByCreatedAtDesc(auditStatus, pageable);
        } else {
            page = jobRepository.findByAuditStatusOrderByCreatedAtDesc("pending", pageable);
        }
        // 批量加载公司信息，避免 N+1 查询
        Map<Long, Company> companyMap = new HashMap<>();
        if (!page.isEmpty()) {
            List<Long> companyIds = page.getContent().stream().map(Job::getCompanyId).distinct().toList();
            companyMap = companyRepository.findAllById(companyIds).stream()
                    .collect(Collectors.toMap(Company::getId, c -> c));
        }
        Map<Long, Company> finalMap = companyMap;
        return page.map(j -> toListResponse(j, finalMap));
    }

    /**
     * 获取职位详情
     *
     * @param id 职位 ID
     * @return 职位详情 DTO
     */
    @Transactional(readOnly = true)
    public JobListResponse getJobDetail(Long id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("职位不存在: " + id));
        Company company = companyRepository.findById(job.getCompanyId()).orElse(null);
        Map<Long, Company> cm = new HashMap<>();
        if (company != null) {
            cm.put(company.getId(), company);
        }
        return toListResponse(job, cm);
    }

    /**
     * 审核通过职位
     *
     * @param adminId 操作管理员 ID
     * @param jobId   职位 ID
     */
    @Transactional
    @CacheEvict(value = {"jobs", "dashboard"}, allEntries = true)
    public void approve(Long adminId, Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("职位不存在: " + jobId));
        job.setAuditStatus("approved");
        jobRepository.save(job);
        auditLogService.log(adminId, "approve_job", "job", jobId, null);
        Company company = companyRepository.findById(job.getCompanyId()).orElse(null);
        if (company != null) {
            notificationService.createSystemNotification(company.getBossId(), "job_approved",
                    "您发布的职位「" + job.getTitle() + "」已通过审核", jobId);
        }
    }

    /**
     * 驳回职位审核
     *
     * @param adminId 操作管理员 ID
     * @param jobId   职位 ID
     * @param reason  驳回原因
     */
    @Transactional
    @CacheEvict(value = {"jobs", "dashboard"}, allEntries = true)
    public void reject(Long adminId, Long jobId, String reason) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("职位不存在: " + jobId));
        job.setAuditStatus("rejected");
        job.setAuditReason(reason);
        jobRepository.save(job);
        auditLogService.log(adminId, "reject_job", "job", jobId, reason);
        Company company = companyRepository.findById(job.getCompanyId()).orElse(null);
        if (company != null) {
            notificationService.createSystemNotification(company.getBossId(), "job_rejected",
                    "您发布的职位「" + job.getTitle() + "」未通过审核，原因：" + reason, jobId);
        }
    }

    /**
     * 批量下架职位
     *
     * 将指定职位列表标记为 rejected 状态，并记录 audit_log。
     * 单个职位操作失败不影响其他职位的处理。
     *
     * @param adminId 操作管理员 ID
     * @param ids     要下架的职位 ID 列表
     * @return 成功下架的数量
     */
    @Transactional
    @CacheEvict(value = {"jobs", "dashboard"}, allEntries = true)
    public int batchOffline(Long adminId, List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            try {
                Job job = jobRepository.findById(id).orElse(null);
                if (job == null) {
                    continue;
                }
                job.setAuditStatus("rejected");
                job.setAuditReason("管理员批量下架");
                job.setStatus("closed");
                jobRepository.save(job);
                auditLogService.log(adminId, "batch_offline", "job", id, "批量下架");
                count++;
            } catch (Exception ignored) {
                // 单个失败不影响批量操作继续
            }
        }
        return count;
    }

    /**
     * 将 Job 实体转换为 JobListResponse DTO
     */
    private JobListResponse toListResponse(Job j, Map<Long, Company> companyMap) {
        JobListResponse r = new JobListResponse();
        r.setId(j.getId());
        r.setTitle(j.getTitle());
        r.setCompanyId(j.getCompanyId());
        r.setSalaryMin(j.getSalaryMin());
        r.setSalaryMax(j.getSalaryMax());
        r.setCity(j.getCity());
        r.setEducation(j.getEducation());
        r.setExperience(j.getExperience());
        r.setDescription(j.getDescription());
        r.setAuditStatus(j.getAuditStatus());
        r.setAuditReason(j.getAuditReason());
        r.setCreatedAt(j.getCreatedAt());
        Company company = companyMap.get(j.getCompanyId());
        r.setCompanyName(company != null ? company.getName() : "未知公司");
        return r;
    }
}
