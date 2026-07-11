package com.weib.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weib.cache.CacheInvalidationService;
import com.weib.cache.CacheKeys;
import com.weib.dto.AppealCreateRequest;
import com.weib.dto.AppealResponse;
import com.weib.entity.SanctionAppeal;
import com.weib.entity.UserSanction;
import com.weib.exception.DuplicateAppealException;
import com.weib.repository.SanctionAppealRepository;
import com.weib.repository.UserRepository;
import com.weib.repository.UserSanctionRepository;
import com.weib.service.admin.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AppealService {
    private final SanctionAppealRepository appealRepository;
    private final UserSanctionRepository sanctionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final SanctionService sanctionService;
    private final CacheInvalidationService cacheInvalidationService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public AppealResponse create(Long userId, AppealCreateRequest request) {
        if (userId == null || !userRepository.existsById(userId)) throw new IllegalArgumentException("请先登录");
        if (request == null || request.sanctionId() == null || request.sanctionId() <= 0) {
            throw new IllegalArgumentException("处罚 ID 无效");
        }
        UserSanction sanction = sanctionRepository.findById(request.sanctionId())
                .orElseThrow(() -> new IllegalArgumentException("处罚记录不存在"));
        if (!userId.equals(sanction.getUserId())) throw new IllegalArgumentException("无权申诉该处罚");
        if (!isActive(sanction)) throw new IllegalArgumentException("仅能申诉当前生效的处罚");
        if (appealRepository.findFirstBySanctionIdAndUserIdAndStatus(
                sanction.getId(), userId, "PENDING").isPresent()) {
            throw new DuplicateAppealException();
        }
        String reason = normalizeReason(request.reason());
        List<String> evidence = validateEvidence(request.evidenceUrls());
        SanctionAppeal appeal = new SanctionAppeal();
        appeal.setSanctionId(sanction.getId());
        appeal.setUserId(userId);
        appeal.setReason(reason);
        appeal.setEvidenceUrls(writeEvidence(evidence));
        appeal.setStatus("PENDING");
        return toResponse(appealRepository.save(appeal));
    }

    @Transactional(readOnly = true)
    public Page<AppealResponse> mine(Long userId, Pageable pageable) {
        if (userId == null) throw new IllegalArgumentException("请先登录");
        return appealRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AppealResponse detailForUser(Long userId, Long id) {
        SanctionAppeal appeal = get(id);
        if (!userId.equals(appeal.getUserId())) throw new IllegalArgumentException("无权查看该申诉");
        return toResponse(appeal);
    }

    @Transactional(readOnly = true)
    public Page<AppealResponse> list(String status, Pageable pageable) {
        String normalized = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        return (normalized == null ? appealRepository.findAll(pageable)
                : appealRepository.findByStatus(normalized, pageable)).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AppealResponse detail(Long id) {
        return toResponse(get(id));
    }

    @Transactional
    public void approve(Long adminId, Long id, String reason) {
        SanctionAppeal appeal = pending(id);
        String reviewReason = normalizeReason(reason);
        UserSanction sanction = sanctionRepository.findById(appeal.getSanctionId())
                .orElseThrow(() -> new IllegalArgumentException("处罚记录不存在"));
        if (!appeal.getUserId().equals(sanction.getUserId())) throw new IllegalArgumentException("申诉与处罚用户不一致");
        sanction.setStatus("REVOKED");
        sanctionRepository.save(sanction);
        appeal.setStatus("APPROVED");
        appeal.setReviewerId(adminId);
        appeal.setReviewReason(reviewReason);
        appeal.setReviewedAt(LocalDateTime.now());
        appealRepository.save(appeal);
        invalidateUser(appeal.getUserId(), sanction.getSanctionType());
        auditLogService.log(adminId, "approve_sanction_appeal", "sanction_appeal", id, reviewReason);
        notificationService.createSystemNotification(appeal.getUserId(), "appeal_approved",
                "你的处罚申诉已通过，相关处罚已撤销", id);
    }

    @Transactional
    public void reject(Long adminId, Long id, String reason) {
        SanctionAppeal appeal = pending(id);
        String reviewReason = normalizeReason(reason);
        appeal.setStatus("REJECTED");
        appeal.setReviewerId(adminId);
        appeal.setReviewReason(reviewReason);
        appeal.setReviewedAt(LocalDateTime.now());
        appealRepository.save(appeal);
        auditLogService.log(adminId, "reject_sanction_appeal", "sanction_appeal", id, reviewReason);
        notificationService.createSystemNotification(appeal.getUserId(), "appeal_rejected",
                "你的处罚申诉未通过审核：" + reviewReason, id);
    }

    private void invalidateUser(Long userId, String type) {
        sanctionService.invalidate(userId, type);
        cacheInvalidationService.invalidate(CacheKeys.userPublic(userId));
    }

    private SanctionAppeal pending(Long id) {
        SanctionAppeal appeal = get(id);
        if (!"PENDING".equals(appeal.getStatus())) throw new IllegalArgumentException("申诉已经处理过了");
        return appeal;
    }

    private SanctionAppeal get(Long id) {
        if (id == null) throw new IllegalArgumentException("申诉 ID 无效");
        return appealRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("申诉不存在"));
    }

    private boolean isActive(UserSanction sanction) {
        if (!"ACTIVE".equalsIgnoreCase(sanction.getStatus())) return false;
        LocalDateTime now = LocalDateTime.now();
        return sanction.getStartsAt() == null || !sanction.getStartsAt().isAfter(now)
                && (sanction.getEndsAt() == null || sanction.getEndsAt().isAfter(now));
    }

    private String normalizeReason(String value) {
        if (value == null || value.trim().length() < 2 || value.trim().length() > 2000) {
            throw new IllegalArgumentException("申诉/审核理由长度必须为 2-2000 个字符");
        }
        return value.trim();
    }

    private List<String> validateEvidence(List<String> evidenceUrls) {
        if (evidenceUrls == null) return List.of();
        if (evidenceUrls.size() > 5) throw new IllegalArgumentException("证据最多上传 5 个");
        return evidenceUrls.stream().map(url -> url == null ? "" : url.trim()).peek(url -> {
            if (url.isBlank() || url.length() > 500
                    || !(url.startsWith("/uploads/") || url.startsWith("http://") || url.startsWith("https://"))) {
                throw new IllegalArgumentException("证据地址无效");
            }
        }).toList();
    }

    private String writeEvidence(List<String> evidence) {
        try { return objectMapper.writeValueAsString(evidence); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("证据格式无效", e); }
    }

    private AppealResponse toResponse(SanctionAppeal appeal) {
        return new AppealResponse(appeal.getId(), appeal.getSanctionId(), appeal.getUserId(), appeal.getReason(),
                readEvidence(appeal.getEvidenceUrls()), appeal.getStatus(), appeal.getReviewReason(),
                appeal.getReviewedAt(), appeal.getCreatedAt());
    }

    private List<String> readEvidence(String value) {
        if (value == null || value.isBlank()) return List.of();
        try { return objectMapper.readValue(value, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)); }
        catch (Exception e) { return List.of(); }
    }
}