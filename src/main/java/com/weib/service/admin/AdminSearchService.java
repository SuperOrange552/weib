package com.weib.service.admin;

import com.weib.dto.admin.AdminSearchResponse;
import com.weib.entity.Company;
import com.weib.entity.Job;
import com.weib.entity.Resume;
import com.weib.entity.User;
import com.weib.repository.CompanyRepository;
import com.weib.repository.JobRepository;
import com.weib.repository.ResumeRepository;
import com.weib.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminSearchService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;

    public AdminSearchService(UserRepository userRepository, CompanyRepository companyRepository,
                              JobRepository jobRepository, ResumeRepository resumeRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminSearchResponse> search(String type, String keyword, Pageable pageable, boolean superAdmin) {
        String normalizedType = normalizeType(type);
        String q = normalizeKeyword(keyword);
        return switch (normalizedType) {
            case "USER" -> userRepository.findByUsernameContainingIgnoreCase(q, pageable).map(u -> user(u, superAdmin));
            case "COMPANY" -> companyRepository.findByNameContainingIgnoreCase(q, pageable).map(this::company);
            case "JOB" -> jobRepository.findByTitleContainingIgnoreCase(q, pageable).map(this::job);
            case "RESUME" -> resumeRepository.search(q, pageable).map(r -> resume(r, superAdmin));
            case "ALL" -> all(q, pageable, superAdmin);
            default -> throw new IllegalArgumentException("检索类型无效");
        };
    }

    @Transactional(readOnly = true)
    public com.weib.dto.admin.AdminSearchDetailResponse detail(String type, Long id, boolean superAdmin) {
        String normalized = normalizeType(type);
        if (id == null) throw new IllegalArgumentException("资源 ID 无效");
        Map<String, Object> data = new LinkedHashMap<>();
        switch (normalized) {
            case "USER" -> userRepository.findById(id).ifPresentOrElse(u -> {
                data.put("id", u.getId()); data.put("username", u.getUsername());
                data.put("nickname", u.getNickname()); data.put("phone", superAdmin ? u.getPhone() : maskPhone(u.getPhone()));
                data.put("role", u.getRole()); data.put("status", u.getStatus()); data.put("createdAt", u.getCreatedAt());
            }, () -> { throw new IllegalArgumentException("用户不存在"); });
            case "COMPANY" -> companyRepository.findById(id).ifPresentOrElse(c -> {
                data.put("id", c.getId()); data.put("name", c.getName()); data.put("logo", c.getLogo());
                data.put("industry", c.getIndustry()); data.put("scale", c.getScale()); data.put("description", c.getDescription());
                data.put("address", c.getAddress()); data.put("auditStatus", c.getAuditStatus()); data.put("bossId", c.getBossId());
            }, () -> { throw new IllegalArgumentException("公司不存在"); });
            case "JOB" -> jobRepository.findById(id).ifPresentOrElse(j -> {
                data.put("id", j.getId()); data.put("title", j.getTitle()); data.put("companyId", j.getCompanyId());
                data.put("city", j.getCity()); data.put("description", j.getDescription()); data.put("requirements", j.getRequirements());
                data.put("auditStatus", j.getAuditStatus()); data.put("status", j.getStatus());
            }, () -> { throw new IllegalArgumentException("职位不存在"); });
            case "RESUME" -> resumeRepository.findById(id).ifPresentOrElse(r -> {
                data.put("id", r.getId()); data.put("userId", r.getUserId()); data.put("realName", r.getRealName());
                data.put("education", r.getEducation()); data.put("school", r.getSchool()); data.put("major", r.getMajor());
                data.put("skills", r.getSkills()); data.put("selfIntroduction", r.getSelfIntroduction()); data.put("status", r.getStatus());
                if (superAdmin) { data.put("phone", r.getPhone()); data.put("email", r.getEmail()); }
                else { data.put("phone", maskPhone(r.getPhone())); data.put("email", maskEmail(r.getEmail())); }
            }, () -> { throw new IllegalArgumentException("简历不存在"); });
            default -> throw new IllegalArgumentException("检索类型无效");
        }
        return new com.weib.dto.admin.AdminSearchDetailResponse(normalized, id, data);
    }

    private Page<AdminSearchResponse> all(String keyword, Pageable pageable, boolean superAdmin) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 50);
        List<AdminSearchResponse> content = new ArrayList<>();
        content.addAll(userRepository.findByUsernameContainingIgnoreCase(keyword, pageable).map(u -> user(u, superAdmin)).getContent());
        content.addAll(companyRepository.findByNameContainingIgnoreCase(keyword, pageable).map(this::company).getContent());
        content.addAll(jobRepository.findByTitleContainingIgnoreCase(keyword, pageable).map(this::job).getContent());
        content.addAll(resumeRepository.search(keyword, pageable).map(r -> resume(r, superAdmin)).getContent());
        content.sort((a, b) -> {
            if (a.createdAt() == null) return 1;
            if (b.createdAt() == null) return -1;
            return b.createdAt().compareTo(a.createdAt());
        });
        int from = Math.min((int) pageable.getOffset(), content.size());
        int to = Math.min(from + size, content.size());
        return new PageImpl<>(content.subList(from, to), pageable, content.size());
    }

    private AdminSearchResponse user(User user, boolean superAdmin) {
        String display = user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
        String phone = superAdmin ? user.getPhone() : maskPhone(user.getPhone());
        return new AdminSearchResponse("USER", user.getId(), user.getId(), display,
                phone, user.getAvatar(), user.getStatus(), user.getCreatedAt());
    }

    private AdminSearchResponse company(Company company) {
        return new AdminSearchResponse("COMPANY", company.getId(), company.getBossId(), company.getName(),
                company.getIndustry(), company.getLogo(), company.getAuditStatus(), company.getCreatedAt());
    }

    private AdminSearchResponse job(Job job) {
        return new AdminSearchResponse("JOB", job.getId(), job.getCompanyId(), job.getTitle(),
                job.getCity(), null, job.getAuditStatus(), job.getCreatedAt());
    }

    private AdminSearchResponse resume(Resume resume, boolean superAdmin) {
        String subtitle = resume.getSchool() == null ? resume.getMajor() : resume.getSchool();
        return new AdminSearchResponse("RESUME", resume.getId(), resume.getUserId(),
                resume.getRealName() == null ? "未填写姓名" : resume.getRealName(), subtitle,
                resume.getAvatar(), resume.getStatus(), resume.getCreatedAt());
    }

    private String normalizeType(String type) {
        return type == null || type.isBlank() ? "ALL" : type.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) throw new IllegalArgumentException("检索关键词不能为空");
        String q = keyword.trim();
        if (q.length() > 80) throw new IllegalArgumentException("检索关键词不能超过 80 个字符");
        return q;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf('@');
        String prefix = email.substring(0, at);
        return (prefix.isEmpty() ? "*" : prefix.substring(0, 1) + "***") + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
