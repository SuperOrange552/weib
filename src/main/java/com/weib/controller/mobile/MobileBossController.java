package com.weib.controller.mobile;

import com.weib.dto.PublicUserProfile;
import com.weib.dto.Result;
import com.weib.entity.Application;
import com.weib.entity.Company;
import com.weib.entity.Job;
import com.weib.entity.Resume;
import com.weib.entity.User;
import com.weib.security.Idempotent;
import com.weib.service.ApplicationService;
import com.weib.service.CompanyService;
import com.weib.service.JobService;
import com.weib.service.NotificationService;
import com.weib.service.ResumeService;
import com.weib.service.SanctionService;
import com.weib.service.UserService;
import com.weib.util.IdObfuscator;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/mobile/boss")
@RequiredArgsConstructor
public class MobileBossController {
    private final CompanyService companyService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final ResumeService resumeService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final SanctionService sanctionService;
    private final IdObfuscator idObfuscator;
    private final MobileAccessPolicy accessPolicy;

    @GetMapping("/dashboard")
    public Result<?> dashboard(HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        Company company = companyOrNull(boss);
        if (company == null) return Result.success(Map.of("companyRegistered", false));
        List<Job> jobs = jobService.getJobsByCompanyId(company.getId());
        List<Application> applications = applicationService.getApplicationsByCompany(company.getId());
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        applications.forEach(app -> statusCounts.merge(app.getStatus(), 1L, Long::sum));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("companyRegistered", true);
        data.put("company", companyJson(company));
        data.put("jobCount", jobs.size());
        data.put("activeJobCount", jobs.stream().filter(j -> "active".equals(j.getStatus())).count());
        data.put("applicationCount", applications.size());
        data.put("applicationStatusCounts", statusCounts);
        return Result.success(data);
    }

    @GetMapping("/company")
    public Result<?> company(HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        Company company = companyOrNull(boss);
        return company == null ? Result.success(Map.of("registered", false)) : Result.success(companyJson(company));
    }

    @PutMapping("/company")
    @Idempotent
    public Result<?> updateCompany(@RequestBody CompanyPayload body, HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        Company company = companyOrNull(boss);
        if (company == null) return Result.error("请先在网页端完成企业入驻审核");
        if (body.industry() != null) company.setIndustry(body.industry());
        if (body.scale() != null) company.setScale(body.scale());
        if (body.address() != null) company.setAddress(body.address());
        if (body.description() != null) company.setDescription(body.description());
        if (body.contactName() != null) company.setContactName(body.contactName());
        if (body.contactPhone() != null) company.setContactPhone(body.contactPhone());
        if (body.contactEmail() != null) company.setContactEmail(body.contactEmail());
        company.setLongitude(body.longitude());
        company.setLatitude(body.latitude());
        return Result.success(companyJson(companyService.updateCompany(company)));
    }

    @GetMapping("/jobs")
    public Result<?> jobs(HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        Company company = companyOrNull(boss);
        if (company == null) return Result.success(List.of());
        return Result.success(jobService.getJobsByCompanyId(company.getId()).stream().map(this::jobJson).toList());
    }

    @PostMapping("/jobs")
    @Idempotent
    public Result<?> createJob(@RequestBody JobPayload body, HttpSession session) {
        return saveJob(null, body, session);
    }

    @PutMapping("/jobs/{jobId}")
    @Idempotent
    public Result<?> updateJob(@PathVariable String jobId, @RequestBody JobPayload body, HttpSession session) {
        return saveJob(jobId, body, session);
    }

    @PostMapping("/jobs/{jobId}/close")
    @Idempotent
    public Result<?> closeJob(@PathVariable String jobId, HttpSession session) {
        return changeJobStatus(jobId, "closed", session);
    }

    @PostMapping("/jobs/{jobId}/reopen")
    @Idempotent
    public Result<?> reopenJob(@PathVariable String jobId, HttpSession session) {
        return changeJobStatus(jobId, "active", session);
    }

    @GetMapping("/jobs/{jobId}/stats")
    public Result<?> jobStats(@PathVariable String jobId, HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        Job job = ownedJob(jobId, boss);
        if (job == null) return Result.error(403, "无权查看该职位");
        Map<String, Long> breakdown = new LinkedHashMap<>();
        applicationService.getApplicationsByJobId(job.getId())
                .forEach(app -> breakdown.merge(app.getStatus(), 1L, Long::sum));
        return Result.success(Map.of("viewCount", job.getViewCount(),
                "applyCount", applicationService.countByJobId(job.getId()), "statusBreakdown", breakdown));
    }

    @GetMapping("/applications")
    public Result<?> applications(HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        Company company = companyOrNull(boss);
        if (company == null) return Result.success(List.of());
        List<Job> jobs = jobService.getJobsByCompanyId(company.getId());
        Map<Long, Job> jobMap = new HashMap<>();
        jobs.forEach(job -> jobMap.put(job.getId(), job));
        List<Application> applications = applicationService.getApplicationsByCompany(company.getId());
        return Result.success(applications.stream().map(app -> applicationJson(app, jobMap.get(app.getJobId()))).toList());
    }

    @GetMapping("/applications/{applicationId}/resume")
    public Result<?> resume(@PathVariable String applicationId, HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        Application application = ownedApplication(applicationId, boss);
        if (application == null) return Result.error(403, "无权查看该简历");
        Resume resume = resumeService.getResumeByUserId(application.getUserId());
        PublicUserProfile profile = userService.getPublicUserProfile(application.getUserId());
        return Result.success(Map.of("resume", resume, "seeker", profile));
    }

    @PostMapping("/applications/{applicationId}/status")
    @Idempotent
    public Result<?> status(@PathVariable String applicationId, @RequestBody StatusPayload body, HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        if (!Set.of("viewed", "interviewing", "offered", "accepted", "rejected").contains(body.status())) {
            return Result.error("投递状态无效");
        }
        Application application = ownedApplication(applicationId, boss);
        if (application == null) return Result.error(403, "无权操作该投递");
        applicationService.updateStatus(application.getId(), body.status(), body.bossNote());
        notificationService.createNotification(application.getUserId(), "application_update",
                "你的投递状态已更新为 " + body.status(), application.getJobId());
        return Result.success(Map.of("status", body.status()));
    }

    @PostMapping("/applications/{applicationId}/interview")
    @Idempotent
    public Result<?> interview(@PathVariable String applicationId, @RequestBody InterviewPayload body,
                               HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        Application application = ownedApplication(applicationId, boss);
        if (application == null) return Result.error(403, "无权操作该投递");
        application.setInterviewTime(LocalDateTime.parse(body.interviewTime()));
        application.setInterviewLocation(body.interviewLocation());
        application.setBossNote(body.bossNote());
        application.setStatus("interviewing");
        applicationService.updateStatus(application);
        notificationService.createNotification(application.getUserId(), "interview_invite",
                "面试邀请：" + body.interviewTime() + "，地点：" + body.interviewLocation(), application.getJobId());
        return Result.success(Map.of("status", "interviewing"));
    }

    private Result<?> saveJob(String encodedId, JobPayload body, HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        sanctionService.assertAllowed(boss.getId(), "PUBLISH_BAN");
        Company company = companyOrNull(boss);
        if (company == null) return Result.error("请先完成企业入驻");
        if (body.salaryMin() != null && body.salaryMax() != null && body.salaryMin() > body.salaryMax()) {
            return Result.error("最低薪资不能高于最高薪资");
        }
        Job job;
        if (encodedId == null) {
            job = new Job();
            job.setCompanyId(company.getId());
            job.setStatus("active");
        } else {
            job = ownedJob(encodedId, boss);
            if (job == null) return Result.error(403, "无权编辑该职位");
        }
        job.setTitle(body.title());
        job.setSalaryMin(body.salaryMin());
        job.setSalaryMax(body.salaryMax());
        job.setCity(body.city());
        job.setAddress(body.address());
        job.setEducation(body.education());
        job.setExperience(body.experience());
        job.setDescription(body.description());
        job.setRequirements(body.requirements());
        job.setTags(body.tags());
        Job saved = encodedId == null ? jobService.createJob(job) : jobService.updateJob(job);
        return Result.success(jobJson(saved));
    }

    private Result<?> changeJobStatus(String encodedId, String status, HttpSession session) {
        User boss = currentBoss(session);
        if (boss == null) return roleError();
        if ("active".equals(status)) sanctionService.assertAllowed(boss.getId(), "PUBLISH_BAN");
        Job job = ownedJob(encodedId, boss);
        if (job == null) return Result.error(403, "无权操作该职位");
        job.setStatus(status);
        jobService.updateJob(job);
        return Result.success(Map.of("status", status));
    }

    private Job ownedJob(String encodedId, User boss) {
        Long id = idObfuscator.decode(encodedId);
        if (id == null) return null;
        try {
            Company company = companyService.getCompanyByBossId(boss.getId());
            Job job = jobService.getJobById(id);
            return accessPolicy.ownsCompanyResource(company.getId(), job.getCompanyId()) ? job : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Application ownedApplication(String encodedId, User boss) {
        Long id = idObfuscator.decode(encodedId);
        if (id == null) return null;
        try {
            Application application = applicationService.getApplicationById(id);
            return ownedJob(idObfuscator.encode(application.getJobId()), boss) == null ? null : application;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private User currentBoss(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return accessPolicy.hasRole(session, "boss") ? user : null;
    }

    private Company companyOrNull(User boss) {
        try { return companyService.getCompanyByBossId(boss.getId()); }
        catch (RuntimeException ex) { return null; }
    }

    private Result<?> roleError() { return Result.error(403, "仅招聘者可以访问该功能"); }

    private Map<String, Object> companyJson(Company c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", idObfuscator.encode(c.getId())); map.put("name", c.getName()); map.put("logo", c.getLogo());
        map.put("industry", c.getIndustry()); map.put("scale", c.getScale()); map.put("description", c.getDescription());
        map.put("address", c.getAddress()); map.put("contactName", c.getContactName()); map.put("contactPhone", c.getContactPhone());
        map.put("contactEmail", c.getContactEmail()); map.put("auditStatus", c.getAuditStatus()); map.put("auditReason", c.getAuditReason());
        map.put("latitude", c.getLatitude()); map.put("longitude", c.getLongitude()); return map;
    }

    private Map<String, Object> jobJson(Job j) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", idObfuscator.encode(j.getId())); map.put("title", j.getTitle()); map.put("salaryMin", j.getSalaryMin());
        map.put("salaryMax", j.getSalaryMax()); map.put("city", j.getCity()); map.put("address", j.getAddress());
        map.put("education", j.getEducation()); map.put("experience", j.getExperience()); map.put("description", j.getDescription());
        map.put("requirements", j.getRequirements()); map.put("tags", j.getTags()); map.put("status", j.getStatus());
        map.put("auditStatus", j.getAuditStatus()); map.put("auditReason", j.getAuditReason()); map.put("viewCount", j.getViewCount()); return map;
    }

    private Map<String, Object> applicationJson(Application app, Job job) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", idObfuscator.encode(app.getId())); map.put("jobId", idObfuscator.encode(app.getJobId()));
        map.put("seekerId", idObfuscator.encode(app.getUserId())); map.put("status", app.getStatus()); map.put("bossNote", app.getBossNote());
        map.put("interviewTime", app.getInterviewTime()); map.put("interviewLocation", app.getInterviewLocation());
        map.put("createdAt", app.getCreatedAt()); map.put("jobTitle", job == null ? "职位已删除" : job.getTitle());
        PublicUserProfile profile = userService.getPublicUserProfile(app.getUserId());
        map.put("seekerName", profile == null ? "求职者" : (profile.nickname() == null ? profile.username() : profile.nickname()));
        map.put("seekerAvatar", profile == null ? null : profile.avatar()); return map;
    }

    public record CompanyPayload(String industry, String scale, String address, String description,
                                 String contactName, String contactPhone, String contactEmail,
                                 Double longitude, Double latitude) {}
    public record JobPayload(String title, Integer salaryMin, Integer salaryMax, String city, String address,
                             String education, String experience, String description, String requirements, String tags) {}
    public record StatusPayload(String status, String bossNote) {}
    public record InterviewPayload(String interviewTime, String interviewLocation, String bossNote) {}
}
