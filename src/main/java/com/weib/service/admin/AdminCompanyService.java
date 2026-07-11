package com.weib.service.admin;

import com.weib.dto.admin.CompanyListResponse;
import com.weib.entity.Company;
import com.weib.entity.User;
import com.weib.repository.CompanyRepository;
import com.weib.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 公司审核业务服务
 *
 * 提供公司审核列表查询、详情查看、审核通过/驳回等业务操作。
 * 每个审核操作均在同一事务内完成：更新状态 + 记录日志 + 发送通知。
 */
@Service
public class AdminCompanyService {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final com.weib.service.NotificationService notificationService;
    private final AdminIdentityService identityService;

    public AdminCompanyService(CompanyRepository companyRepository, UserRepository userRepository,
                               AuditLogService auditLogService,
                               com.weib.service.NotificationService notificationService,
                               AdminIdentityService identityService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.identityService = identityService;
    }

    /**
     * 分页查询公司列表（支持审核状态筛选 + 关键词搜索）
     *
     * @param auditStatus 审核状态（pending/approved/rejected），默认 pending
     * @param keyword     搜索关键词（模糊匹配公司名）
     * @param pageable    分页参数
     * @return 分页公司列表
     */
    @Transactional(readOnly = true)
    public Page<CompanyListResponse> listCompanies(String auditStatus, String keyword, Pageable pageable) {
        Page<Company> page;
        if (keyword != null && !keyword.isBlank()) {
            if (auditStatus != null && !auditStatus.isBlank()) {
                page = companyRepository.findByNameContainingIgnoreCaseAndAuditStatus(keyword, auditStatus, pageable);
            } else {
                // 无状态筛选时搜索所有公司（不限审核状态）
                page = companyRepository.findByNameContainingIgnoreCase(keyword, pageable);
            }
        } else if (auditStatus != null && !auditStatus.isBlank()) {
            page = companyRepository.findByAuditStatusOrderByCreatedAtDesc(auditStatus, pageable);
        } else {
            page = companyRepository.findByAuditStatusOrderByCreatedAtDesc("pending", pageable);
        }
        return page.map(this::toListResponse);
    }

    /**
     * 获取公司详情
     *
     * @param id 公司 ID
     * @return 公司详情 DTO
     */
    @Transactional(readOnly = true)
    public CompanyListResponse getCompanyDetail(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公司不存在: " + id));
        return toListResponse(company);
    }

    /**
     * 审核通过公司
     *
     * @param adminId   操作管理员 ID
     * @param companyId 公司 ID
     */
    @Transactional
    @CacheEvict(value = {"companies", "dashboard"}, allEntries = true)
    public void approve(Long adminId, Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("公司不存在: " + companyId));
        company.setAuditStatus("approved");
        companyRepository.save(company);
        identityService.enable(adminId, company.getBossId(), "BOSS", "企业审核通过 companyId=" + companyId);
        auditLogService.log(adminId, "approve_company", "company", companyId, null);
        notificationService.createSystemNotification(company.getBossId(), "company_approved",
                "您的公司「" + company.getName() + "」已通过审核", companyId);
    }

    /**
     * 驳回公司审核
     *
     * @param adminId   操作管理员 ID
     * @param companyId 公司 ID
     * @param reason    驳回原因
     */
    @Transactional
    @CacheEvict(value = {"companies", "dashboard"}, allEntries = true)
    public void reject(Long adminId, Long companyId, String reason) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("公司不存在: " + companyId));
        company.setAuditStatus("rejected");
        company.setAuditReason(reason);
        companyRepository.save(company);
        auditLogService.log(adminId, "reject_company", "company", companyId, reason);
        notificationService.createSystemNotification(company.getBossId(), "company_rejected",
                "您的公司「" + company.getName() + "」未通过审核，原因：" + reason, companyId);
    }

    /**
     * 将 Company 实体转换为 CompanyListResponse DTO
     */
    private CompanyListResponse toListResponse(Company c) {
        CompanyListResponse r = new CompanyListResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setIndustry(c.getIndustry());
        r.setScale(c.getScale());
        r.setAddress(c.getAddress());
        r.setDescription(c.getDescription());
        r.setAuditStatus(c.getAuditStatus());
        r.setAuditReason(c.getAuditReason());
        r.setCreatedAt(c.getCreatedAt());
        try {
            User boss = userRepository.findById(c.getBossId()).orElse(null);
            r.setBossName(boss != null ? (boss.getNickname() != null ? boss.getNickname() : boss.getUsername()) : "未知");
        } catch (Exception e) {
            r.setBossName("未知");
        }
        return r;
    }
}
