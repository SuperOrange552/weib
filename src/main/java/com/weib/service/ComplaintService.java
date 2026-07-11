package com.weib.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weib.dto.ComplaintCreateRequest;
import com.weib.dto.ComplaintResponse;
import com.weib.entity.Company;
import com.weib.entity.Complaint;
import com.weib.entity.Job;
import com.weib.entity.Resume;
import com.weib.exception.DuplicateComplaintException;
import com.weib.repository.CompanyRepository;
import com.weib.repository.ComplaintRepository;
import com.weib.repository.JobRepository;
import com.weib.repository.ResumeRepository;
import com.weib.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 投诉提交、对象校验和用户投诉查询。 */
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private static final Set<String> TARGET_TYPES = Set.of("USER", "JOB", "COMPANY", "RESUME", "MEDIA");
    private static final Set<String> CATEGORIES = Set.of(
            "FAKE_JOB", "FAKE_PHOTO", "FRAUD", "HARASSMENT", "SPAM", "ILLEGAL", "OTHER");

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ComplaintResponse create(Long reporterId, ComplaintCreateRequest request) {
        if (reporterId == null || !userRepository.existsById(reporterId)) {
            throw new IllegalArgumentException("请先登录");
        }
        String targetType = normalize(request == null ? null : request.targetType());
        String category = normalize(request == null ? null : request.category());
        Long targetId = request == null ? null : request.targetId();
        if (!TARGET_TYPES.contains(targetType)) throw new IllegalArgumentException("投诉对象类型无效");
        if (!CATEGORIES.contains(category)) throw new IllegalArgumentException("投诉类型无效");
        if (targetId == null || targetId <= 0) throw new IllegalArgumentException("投诉对象无效");

        String description = request.description() == null ? "" : request.description().trim();
        if (description.length() < 2 || description.length() > 2000) {
            throw new IllegalArgumentException("投诉说明长度必须为 2-2000 个字符");
        }
        List<String> evidence = validateEvidence(request.evidenceUrls());
        validateTarget(reporterId, targetType, targetId);
        if (complaintRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporterId, targetType, targetId, "PENDING")) {
            throw new DuplicateComplaintException();
        }

        Complaint complaint = new Complaint();
        complaint.setReporterId(reporterId);
        complaint.setTargetType(targetType);
        complaint.setTargetId(targetId);
        complaint.setCategory(category);
        complaint.setDescription(description);
        complaint.setEvidenceUrls(writeEvidence(evidence));
        complaint.setStatus("PENDING");
        return toResponse(complaintRepository.save(complaint));
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> mine(Long reporterId) {
        if (reporterId == null) throw new IllegalArgumentException("请先登录");
        return complaintRepository.findByReporterIdOrderByCreatedAtDesc(reporterId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getMine(Long reporterId, Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("投诉不存在"));
        if (!reporterId.equals(complaint.getReporterId())) {
            throw new IllegalArgumentException("无权查看该投诉");
        }
        return toResponse(complaint);
    }

    private void validateTarget(Long reporterId, String targetType, Long targetId) {
        switch (targetType) {
            case "USER" -> {
                if (reporterId.equals(targetId)) throw new IllegalArgumentException("不能投诉自己");
                if (!userRepository.existsById(targetId)) throw new IllegalArgumentException("用户不存在");
            }
            case "JOB" -> {
                Job job = jobRepository.findById(targetId).orElseThrow(() -> new IllegalArgumentException("职位不存在"));
                Company company = companyRepository.findById(job.getCompanyId())
                        .orElseThrow(() -> new IllegalArgumentException("职位所属公司不存在"));
                if (reporterId.equals(company.getBossId())) throw new IllegalArgumentException("不能投诉自己发布的职位");
            }
            case "COMPANY" -> {
                Company company = companyRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("公司不存在"));
                if (reporterId.equals(company.getBossId())) throw new IllegalArgumentException("不能投诉自己的公司");
            }
            case "RESUME" -> {
                Resume resume = resumeRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("简历不存在"));
                if (reporterId.equals(resume.getUserId())) throw new IllegalArgumentException("不能投诉自己的简历");
            }
            case "MEDIA" -> {
                boolean exists = jobRepository.existsById(targetId)
                        || companyRepository.existsById(targetId)
                        || resumeRepository.existsById(targetId);
                if (!exists) throw new IllegalArgumentException("图片所属对象不存在");
            }
            default -> throw new IllegalArgumentException("投诉对象类型无效");
        }
    }

    private List<String> validateEvidence(List<String> evidenceUrls) {
        if (evidenceUrls == null) return List.of();
        if (evidenceUrls.size() > 5) throw new IllegalArgumentException("证据最多上传 5 个");
        return evidenceUrls.stream().map(url -> url == null ? "" : url.trim()).peek(url -> {
            if (url.isBlank() || url.length() > 500 ||
                    !(url.startsWith("/uploads/") || url.startsWith("http://") || url.startsWith("https://"))) {
                throw new IllegalArgumentException("证据地址无效");
            }
        }).toList();
    }

    private String writeEvidence(List<String> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("证据格式无效", e);
        }
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        return new ComplaintResponse(complaint.getId(), complaint.getTargetType(), complaint.getTargetId(),
                complaint.getCategory(), complaint.getDescription(), readEvidence(complaint.getEvidenceUrls()),
                complaint.getStatus(), complaint.getReviewReason(), complaint.getCreatedAt(), complaint.getReviewedAt());
    }

    private List<String> readEvidence(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
