package com.weib.service.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weib.cache.CacheInvalidationService;
import com.weib.cache.CacheKeys;
import com.weib.dto.admin.ComplaintListResponse;
import com.weib.dto.admin.ComplaintReviewRequest;
import com.weib.dto.admin.SanctionCreateRequest;
import com.weib.dto.admin.SanctionResponse;
import com.weib.entity.*;
import com.weib.repository.*;
import com.weib.service.NotificationService;
import com.weib.service.SanctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 管理后台投诉审核、内容下架和用户处罚。 */
@Service
@RequiredArgsConstructor
public class AdminComplaintService {

    private static final Set<String> SANCTION_TYPES = Set.of("MUTE", "PUBLISH_BAN", "ACCOUNT_BAN");
    private final ComplaintRepository complaintRepository;
    private final UserSanctionRepository sanctionRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final SanctionService sanctionService;
    private final CacheInvalidationService cacheInvalidationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Page<ComplaintListResponse> list(String status, Pageable pageable) {
        Page<Complaint> page = status == null || status.isBlank()
                ? complaintRepository.findAll(pageable)
                : complaintRepository.findByStatus(status.trim().toUpperCase(Locale.ROOT), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SanctionResponse> listSanctions(Pageable pageable) {
        return sanctionRepository.findAll(pageable).map(this::toSanctionResponse);
    }

    @Transactional(readOnly = true)
    public ComplaintListResponse detail(Long id) {
        return toResponse(complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("投诉不存在")));
    }

    @Transactional
    public void reject(Long adminId, Long complaintId, String reason) {
        Complaint complaint = pending(complaintId);
        String normalizedReason = requireReason(reason);
        complaint.setStatus("REJECTED");
        complaint.setReviewerId(adminId);
        complaint.setReviewReason(normalizedReason);
        complaint.setReviewedAt(LocalDateTime.now());
        complaintRepository.save(complaint);
        auditLogService.log(adminId, "reject_complaint", "complaint", complaintId, normalizedReason);
        notificationService.createSystemNotification(complaint.getReporterId(), "complaint_rejected",
                "你的投诉未通过审核：" + normalizedReason, complaintId);
    }

    @Transactional
    public void resolve(Long adminId, Long complaintId, ComplaintReviewRequest request) {
        Complaint complaint = pending(complaintId);
        String reason = requireReason(request == null ? null : request.reason());
        if (request != null && request.contentAction() != null
                && "OFFLINE".equalsIgnoreCase(request.contentAction())) {
            offlineContent(complaint, reason);
        }
        if (request != null && request.sanction() != null) {
            createSanction(adminId, request.sanction(), complaint);
        }
        complaint.setStatus("RESOLVED");
        complaint.setReviewerId(adminId);
        complaint.setReviewReason(reason);
        complaint.setReviewedAt(LocalDateTime.now());
        complaintRepository.save(complaint);
        auditLogService.log(adminId, "resolve_complaint", "complaint", complaintId, reason);
        notificationService.createSystemNotification(complaint.getReporterId(), "complaint_resolved",
                "你的投诉已处理：" + reason, complaintId);
    }

    @Transactional
    public SanctionResponse createSanction(Long adminId, SanctionCreateRequest request) {
        return toSanctionResponse(createSanctionEntity(adminId, request, null));
    }

    @Transactional
    public void revoke(Long adminId, Long sanctionId, String reason) {
        UserSanction sanction = sanctionRepository.findById(sanctionId)
                .orElseThrow(() -> new IllegalArgumentException("处罚记录不存在"));
        if (!"ACTIVE".equals(sanction.getStatus())) throw new IllegalArgumentException("处罚已不是生效状态");
        sanction.setStatus("REVOKED");
        sanctionRepository.save(sanction);
        sanctionService.invalidate(sanction.getUserId(), sanction.getSanctionType());
        cacheInvalidationService.invalidate(CacheKeys.userPublic(sanction.getUserId()));
        auditLogService.log(adminId, "revoke_sanction", "user", sanction.getUserId(), requireReason(reason));
        notificationService.createSystemNotification(sanction.getUserId(), "sanction_revoked",
                "你的账号处罚已撤销", sanction.getId());
    }

    private UserSanction createSanction(Long adminId, SanctionCreateRequest request, Complaint complaint) {
        if (request == null) throw new IllegalArgumentException("处罚参数不能为空");
        Long userId = request.userId() != null ? request.userId() : ownerOf(complaint);
        SanctionCreateRequest normalized = new SanctionCreateRequest(userId, request.sanctionType(),
                request.targetType(), request.targetId(), complaint == null ? request.sourceComplaintId() : complaint.getId(),
                request.reason(), request.startsAt(), request.endsAt());
        UserSanction saved = createSanctionEntity(adminId, normalized, complaint);
        return saved;
    }

    private UserSanction createSanctionEntity(Long adminId, SanctionCreateRequest request, Complaint complaint) {
        String type = request.sanctionType() == null ? "" : request.sanctionType().trim().toUpperCase(Locale.ROOT);
        if (!SANCTION_TYPES.contains(type)) throw new IllegalArgumentException("处罚类型无效");
        if (request.userId() == null || !userRepository.existsById(request.userId())) throw new IllegalArgumentException("被处罚用户不存在");
        String reason = requireReason(request.reason());
        LocalDateTime starts = request.startsAt() == null ? LocalDateTime.now() : request.startsAt();
        if (request.endsAt() != null && !request.endsAt().isAfter(starts)) throw new IllegalArgumentException("处罚结束时间必须晚于开始时间");

        UserSanction sanction = new UserSanction();
        sanction.setUserId(request.userId());
        sanction.setSanctionType(type);
        sanction.setTargetType(request.targetType());
        sanction.setTargetId(request.targetId());
        sanction.setSourceComplaintId(complaint == null ? request.sourceComplaintId() : complaint.getId());
        sanction.setReason(reason);
        sanction.setStartsAt(starts);
        sanction.setEndsAt(request.endsAt());
        sanction.setStatus("ACTIVE");
        sanction.setAdminId(adminId);
        UserSanction saved = sanctionRepository.save(sanction);
        sanctionService.invalidate(saved.getUserId(), saved.getSanctionType());
        cacheInvalidationService.invalidate(CacheKeys.userPublic(saved.getUserId()));
        auditLogService.log(adminId, "create_sanction", "user", saved.getUserId(), reason);
        notificationService.createSystemNotification(saved.getUserId(), "sanction_created",
                "你的账号收到平台处罚：" + type, saved.getId());
        return saved;
    }

    private void offlineContent(Complaint complaint, String reason) {
        switch (complaint.getTargetType()) {
            case "JOB" -> jobRepository.findById(complaint.getTargetId()).ifPresent(job -> {
                job.setStatus("closed");
                job.setAuditStatus("rejected");
                job.setAuditReason(reason);
                jobRepository.save(job);
                cacheInvalidationService.invalidate(CacheKeys.job(job.getId()));
                cacheInvalidationService.invalidatePattern("cache:jobs:list:*");
            });
            case "COMPANY" -> companyRepository.findById(complaint.getTargetId()).ifPresent(company -> {
                company.setAuditStatus("rejected");
                company.setAuditReason(reason);
                companyRepository.save(company);
                cacheInvalidationService.invalidate(CacheKeys.company(company.getId()),
                        CacheKeys.companyByBoss(company.getBossId()));
            });
            case "RESUME" -> resumeRepository.findById(complaint.getTargetId()).ifPresent(resume -> {
                resume.setStatus("hidden");
                resumeRepository.save(resume);
                cacheInvalidationService.invalidate(CacheKeys.resumePublic(resume.getId()),
                        CacheKeys.resumeByUser(resume.getUserId()));
            });
            case "MEDIA" -> offlineMedia(complaint.getTargetId(), reason);
            default -> { }
        }
    }

    private void offlineMedia(Long targetId, String reason) {
        jobRepository.findById(targetId).ifPresent(job -> {
            job.setStatus("closed");
            job.setAuditStatus("rejected");
            job.setAuditReason(reason);
            jobRepository.save(job);
            cacheInvalidationService.invalidate(CacheKeys.job(job.getId()));
        });
        companyRepository.findById(targetId).ifPresent(company -> {
            company.setAuditStatus("rejected");
            company.setAuditReason(reason);
            companyRepository.save(company);
            cacheInvalidationService.invalidate(CacheKeys.company(company.getId()));
        });
        resumeRepository.findById(targetId).ifPresent(resume -> {
            resume.setStatus("hidden");
            resumeRepository.save(resume);
            cacheInvalidationService.invalidate(CacheKeys.resumePublic(resume.getId()));
        });
    }

    private Long ownerOf(Complaint complaint) {
        if (complaint == null) throw new IllegalArgumentException("处罚缺少目标用户");
        return switch (complaint.getTargetType()) {
            case "USER" -> complaint.getTargetId();
            case "COMPANY" -> companyRepository.findById(complaint.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("公司不存在")).getBossId();
            case "JOB" -> {
                Job job = jobRepository.findById(complaint.getTargetId())
                        .orElseThrow(() -> new IllegalArgumentException("职位不存在"));
                yield companyRepository.findById(job.getCompanyId())
                        .orElseThrow(() -> new IllegalArgumentException("公司不存在")).getBossId();
            }
            case "RESUME" -> resumeRepository.findById(complaint.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("简历不存在")).getUserId();
            default -> throw new IllegalArgumentException("媒体投诉需要明确 userId");
        };
    }

    private Complaint pending(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("投诉不存在"));
        if (!"PENDING".equals(complaint.getStatus())) throw new IllegalArgumentException("投诉已经处理过了");
        return complaint;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 2000) {
            throw new IllegalArgumentException("处理理由不能为空且不能超过 2000 字");
        }
        return reason.trim();
    }

    private ComplaintListResponse toResponse(Complaint complaint) {
        return new ComplaintListResponse(complaint.getId(), complaint.getReporterId(), complaint.getTargetType(),
                complaint.getTargetId(), complaint.getCategory(), complaint.getDescription(), readEvidence(complaint.getEvidenceUrls()),
                complaint.getStatus(), complaint.getReviewReason(), complaint.getCreatedAt(), complaint.getReviewedAt());
    }

    private List<String> readEvidence(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    private SanctionResponse toSanctionResponse(UserSanction sanction) {
        return new SanctionResponse(sanction.getId(), sanction.getUserId(), sanction.getSanctionType(),
                sanction.getTargetType(), sanction.getTargetId(), sanction.getReason(), sanction.getStartsAt(),
                sanction.getEndsAt(), sanction.getStatus(), sanction.getAdminId());
    }
}
