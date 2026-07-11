package com.weib.controller;

import com.weib.dto.Result;
import com.weib.entity.*;
import com.weib.service.*;
import com.weib.util.IdObfuscator;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================
 * 【SeekerApiController】求职者端 RESTful JSON API
 * ============================================
 *
 * 用途：为 Android/iOS App 提供纯 JSON 数据接口
 *
 * 与现有 Controller 的关系：
 * - 现有 Controller（IndexController/JobController 等）→ 返回 Thymeleaf HTML（SSR）
 * - 本 Controller → 返回 JSON，给 App 端用
 * - 两者**复用同一套 Service 层**，数据一致性由 Service 保证
 *
 * 路径规划：
 * - 所有接口统一在 /api/seeker/ 下，避免与现有 HTML 路由冲突
 * - CSRF 排除：已在 WebConfig 中 exclude /api/seeker/**
 * - 登录保护：由 LoginInterceptor 统一处理（支持 JWT + Session）
 *
 * 遵循原则（来自系统规则）：
 * - 最小影响原则：不改动任何现有文件，仅新增 + 增加 CSRF 排除
 * - 复用优先：不新增 Service 方法，完全复用现有 Service 层
 * - 数据安全：ID 混淆 + 角色校验 + 水平越权防护
 */
@Controller
@RequiredArgsConstructor
public class SeekerApiController {

    private final JobService jobService;
    private final CompanyService companyService;
    private final ApplicationService applicationService;
    private final FavoriteJobService favoriteJobService;
    private final ResumeService resumeService;
    private final NotificationService notificationService;
    private final MessageService messageService;
    private final IdObfuscator idObfuscator;

    // ============================================
    // 通用工具方法
    // ============================================

    /** 从 Session 提取用户，验证已登录且角色为 seeker */
    private User requireSeeker(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"seeker".equals(user.getRole())) {
            return null;
        }
        return user;
    }

    /** 构建职位 JSON 对象（含公司和收藏/投递状态） */
    private Map<String, Object> buildJobJson(Job job, Map<Long, Company> companyMap,
                                              Set<Long> appliedJobIds, Set<Long> favoritedJobIds) {
        Company company = companyMap.get(job.getCompanyId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", idObfuscator.encode(job.getId()));
        item.put("title", job.getTitle());
        item.put("salaryMin", job.getSalaryMin());
        item.put("salaryMax", job.getSalaryMax());
        item.put("education", job.getEducation());
        item.put("experience", job.getExperience());
        item.put("city", job.getCity());
        item.put("address", job.getAddress());
        item.put("tags", job.getTags());
        item.put("viewCount", job.getViewCount());
        item.put("createdAt", job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
        item.put("updatedAt", job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null);

        if (company != null) {
            Map<String, Object> companyJson = new LinkedHashMap<>();
            companyJson.put("id", idObfuscator.encode(company.getId()));
            companyJson.put("name", company.getName());
            companyJson.put("logo", company.getLogo());
            companyJson.put("industry", company.getIndustry());
            companyJson.put("scale", company.getScale());
            companyJson.put("address", company.getAddress());
            companyJson.put("contactName", company.getContactName());
            item.put("company", companyJson);
        }

        item.put("hasApplied", appliedJobIds.contains(job.getId()));
        item.put("isFavorited", favoritedJobIds.contains(job.getId()));
        return item;
    }

    // ============================================
    // 1. 职位列表（分页 + 搜索 + 筛选）
    // GET /api/seeker/jobs
    // ============================================
    @GetMapping("/api/seeker/jobs")
    @ResponseBody
    public Result<Map<String, Object>> jobList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) Integer salaryMin,
            @RequestParam(required = false) Integer salaryMax,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            HttpSession session) {

        // 参数校验
        if (page < 0) page = 0;
        if (size < 1) size = 12;
        if (size > 100) size = 100;

        // 查询职位（复用 IndexController 的 Service 调用逻辑）
        Page<Job> jobPage;
        boolean hasFilters = (keyword != null && !keyword.isBlank())
                || (city != null && !city.isBlank())
                || (education != null && !education.isBlank())
                || (experience != null && !experience.isBlank())
                || salaryMin != null || salaryMax != null;
        if (hasFilters) {
            jobPage = jobService.searchJobsPaged(keyword, city, education, experience,
                    salaryMin, salaryMax, sort, page, size);
        } else {
            jobPage = jobService.getActiveJobsPaged(page, size);
        }

        List<Job> jobs = jobPage.getContent();

        // 批量加载公司信息
        List<Long> companyIds = jobs.stream()
                .map(Job::getCompanyId).distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyService.getCompanyMapByIds(companyIds);

        // 登录用户查询已投递/已收藏状态
        Set<Long> appliedJobIds = Set.of();
        Set<Long> favoritedJobIds = Set.of();
        User user = (User) session.getAttribute("user");
        if (user != null && "seeker".equals(user.getRole())) {
            List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());
            if (!jobIds.isEmpty()) {
                appliedJobIds = applicationService.getApplicationsByJobIds(jobIds).stream()
                        .filter(a -> a.getUserId().equals(user.getId()))
                        .map(Application::getJobId)
                        .collect(Collectors.toSet());
                favoritedJobIds = favoriteJobService.getUserFavorites(user.getId()).stream()
                        .map(FavoriteJob::getJobId)
                        .filter(jobIds::contains)
                        .collect(Collectors.toSet());
            }
        }

        // 构建返回数据
        List<Map<String, Object>> content = new ArrayList<>();
        for (Job job : jobs) {
            content.add(buildJobJson(job, companyMap, appliedJobIds, favoritedJobIds));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", content);
        data.put("totalElements", jobPage.getTotalElements());
        data.put("totalPages", jobPage.getTotalPages());
        data.put("number", jobPage.getNumber());
        data.put("size", jobPage.getSize());
        data.put("first", jobPage.isFirst());
        data.put("last", jobPage.isLast());
        data.put("empty", jobPage.isEmpty());

        return Result.success(data);
    }

    // ============================================
    // 2. 职位详情
    // GET /api/seeker/job/{encodedId}
    // ============================================
    @GetMapping("/api/seeker/job/{encodedId}")
    @ResponseBody
    public Result<Map<String, Object>> jobDetail(@PathVariable String encodedId, HttpSession session) {
        Long id = idObfuscator.decode(encodedId);
        if (id == null) return Result.error("参数无效");

        try {
            Job job = jobService.getJobById(id);
            if (!"active".equals(job.getStatus())) {
                return Result.error("该职位已关闭");
            }

            Company company = companyService.getCompanyById(job.getCompanyId());

            // 增加浏览量
            jobService.incrementViewCount(id);

            // 登录用户查询投递/收藏状态
            boolean hasApplied = false;
            boolean isFavorited = false;
            User user = (User) session.getAttribute("user");
            if (user != null && "seeker".equals(user.getRole())) {
                hasApplied = jobService.hasApplied(id, user.getId());
                isFavorited = favoriteJobService.isFavorited(id, user.getId());
            }

            Map<String, Object> data = buildJobJson(job,
                    Map.of(company.getId(), company),
                    hasApplied ? Set.of(id) : Set.of(),
                    isFavorited ? Set.of(id) : Set.of());

            return Result.success(data);

        } catch (Exception e) {
            return Result.error("职位不存在");
        }
    }

    // ============================================
    // 3. 公司详情 + 该公司所有职位
    // GET /api/seeker/company/{encodedId}
    // ============================================
    @GetMapping("/api/seeker/company/{encodedId}")
    @ResponseBody
    public Result<Map<String, Object>> companyDetail(@PathVariable String encodedId, HttpSession session) {
        Long id = idObfuscator.decode(encodedId);
        if (id == null) return Result.error("参数无效");

        try {
            Company company = companyService.getCompanyById(id);
            List<Job> jobs = jobService.getJobsByCompanyId(id).stream()
                    .filter(j -> "active".equals(j.getStatus()))
                    .collect(Collectors.toList());

            // 登录用户查询投递/收藏状态
            Set<Long> appliedJobIds = Set.of();
            Set<Long> favoritedJobIds = Set.of();
            User user = (User) session.getAttribute("user");
            if (user != null && "seeker".equals(user.getRole()) && !jobs.isEmpty()) {
                List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());
                appliedJobIds = applicationService.getApplicationsByJobIds(jobIds).stream()
                        .filter(a -> a.getUserId().equals(user.getId()))
                        .map(Application::getJobId)
                        .collect(Collectors.toSet());
                favoritedJobIds = favoriteJobService.getUserFavorites(user.getId()).stream()
                        .map(FavoriteJob::getJobId)
                        .filter(jobIds::contains)
                        .collect(Collectors.toSet());
            }

            Map<Long, Company> companyMap = Map.of(company.getId(), company);
            List<Map<String, Object>> jobList = new ArrayList<>();
            for (Job job : jobs) {
                jobList.add(buildJobJson(job, companyMap, appliedJobIds, favoritedJobIds));
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", idObfuscator.encode(company.getId()));
            data.put("name", company.getName());
            data.put("logo", company.getLogo());
            data.put("industry", company.getIndustry());
            data.put("scale", company.getScale());
            data.put("address", company.getAddress());
            data.put("description", company.getDescription());
            data.put("contactName", company.getContactName());
            data.put("longitude", company.getLongitude());
            data.put("latitude", company.getLatitude());
            data.put("createdAt", company.getCreatedAt() != null ? company.getCreatedAt().toString() : null);
            data.put("jobs", jobList);

            return Result.success(data);

        } catch (Exception e) {
            return Result.error("公司不存在");
        }
    }

    // ============================================
    // 4. 我的投递列表
    // GET /api/seeker/applications
    // ============================================
    @GetMapping("/api/seeker/applications")
    @ResponseBody
    public Result<List<Map<String, Object>>> myApplications(HttpSession session) {
        User user = requireSeeker(session);
        if (user == null) return Result.error("请先以求职者身份登录");

        List<Application> applications = applicationService.getApplicationsByUser(user.getId());

        // 批量加载职位和公司信息
        List<Long> jobIds = applications.stream()
                .map(Application::getJobId).distinct().collect(Collectors.toList());
        Map<Long, Job> jobMap = new HashMap<>();
        if (!jobIds.isEmpty()) {
            jobService.getJobsByIds(jobIds).forEach(j -> jobMap.put(j.getId(), j));
        }

        List<Long> companyIds = jobMap.values().stream()
                .map(Job::getCompanyId).distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyService.getCompanyMapByIds(companyIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Application app : applications) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", idObfuscator.encode(app.getId()));
            item.put("jobId", idObfuscator.encode(app.getJobId()));
            item.put("status", app.getStatus());
            item.put("bossNote", app.getBossNote());
            item.put("interviewTime", app.getInterviewTime() != null ? app.getInterviewTime().toString() : null);
            item.put("interviewLocation", app.getInterviewLocation());
            item.put("rejectReason", app.getRejectReason());
            item.put("createdAt", app.getCreatedAt() != null ? app.getCreatedAt().toString() : null);

            Job job = jobMap.get(app.getJobId());
            if (job != null) {
                item.put("jobTitle", job.getTitle());
                Company company = companyMap.get(job.getCompanyId());
                if (company != null) {
                    item.put("companyName", company.getName());
                    item.put("encodedCompanyId", idObfuscator.encode(company.getId()));
                }
            } else {
                item.put("jobTitle", "职位已删除");
                item.put("companyName", "");
            }
            result.add(item);
        }

        return Result.success(result);
    }

    // ============================================
    // 5. 我的收藏列表
    // GET /api/seeker/favorites
    // ============================================
    @GetMapping("/api/seeker/favorites")
    @ResponseBody
    public Result<List<Map<String, Object>>> myFavorites(HttpSession session) {
        User user = requireSeeker(session);
        if (user == null) return Result.error("请先以求职者身份登录");

        List<FavoriteJob> favorites = favoriteJobService.getUserFavorites(user.getId());
        List<Long> favJobIds = favorites.stream()
                .map(FavoriteJob::getJobId).distinct().collect(Collectors.toList());

        // 只返回活跃职位
        Map<Long, Job> jobMap = new HashMap<>();
        if (!favJobIds.isEmpty()) {
            jobService.getJobsByIds(favJobIds).stream()
                    .filter(j -> "active".equals(j.getStatus()))
                    .forEach(j -> jobMap.put(j.getId(), j));
        }

        List<Long> companyIds = jobMap.values().stream()
                .map(Job::getCompanyId).distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyService.getCompanyMapByIds(companyIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (FavoriteJob fav : favorites) {
            Job job = jobMap.get(fav.getJobId());
            if (job == null) continue; // 已下架职位不显示

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("favoriteId", idObfuscator.encode(fav.getId()));
            item.put("jobId", idObfuscator.encode(job.getId()));
            item.put("title", job.getTitle());
            item.put("salaryMin", job.getSalaryMin());
            item.put("salaryMax", job.getSalaryMax());
            item.put("city", job.getCity());
            item.put("education", job.getEducation());
            item.put("experience", job.getExperience());
            item.put("tags", job.getTags());
            item.put("createdAt", job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);

            Company company = companyMap.get(job.getCompanyId());
            if (company != null) {
                item.put("companyName", company.getName());
                item.put("encodedCompanyId", idObfuscator.encode(company.getId()));
            }
            result.add(item);
        }

        return Result.success(result);
    }

    // ============================================
    // 6. 查看简历
    // GET /api/seeker/resume
    // ============================================
    @GetMapping("/api/seeker/resume")
    @ResponseBody
    public Result<Map<String, Object>> viewResume(HttpSession session) {
        User user = requireSeeker(session);
        if (user == null) return Result.error("请先以求职者身份登录");

        try {
            Resume resume = resumeService.getResumeByUserId(user.getId());
            Map<String, Object> data = buildResumeJson(resume);
            return Result.success(data);
        } catch (Exception e) {
            // 没有简历，返回空
            Map<String, Object> emptyResume = new LinkedHashMap<>();
            emptyResume.put("exists", false);
            emptyResume.put("userId", user.getId());
            return Result.success(emptyResume);
        }
    }

    /** 构建简历 JSON */
    private Map<String, Object> buildResumeJson(Resume resume) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", resume.getId());
        data.put("userId", resume.getUserId());
        data.put("realName", resume.getRealName());
        data.put("gender", resume.getGender());
        data.put("phone", resume.getPhone());
        data.put("email", resume.getEmail());
        data.put("birthday", resume.getBirthday());
        data.put("education", resume.getEducation());
        data.put("school", resume.getSchool());
        data.put("major", resume.getMajor());
        data.put("workExperience", resume.getWorkExperience());
        data.put("projectExperience", resume.getProjectExperience());
        data.put("skills", resume.getSkills());
        data.put("selfIntroduction", resume.getSelfIntroduction());
        data.put("status", resume.getStatus());
        data.put("createdAt", resume.getCreatedAt() != null ? resume.getCreatedAt().toString() : null);
        data.put("updatedAt", resume.getUpdatedAt() != null ? resume.getUpdatedAt().toString() : null);
        data.put("exists", true);
        return data;
    }

    // ============================================
    // 7. 保存简历
    // POST /api/seeker/resume
    // ============================================
    @PostMapping("/api/seeker/resume")
    @ResponseBody
    public Result<Map<String, Object>> saveResume(
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        User user = requireSeeker(session);
        if (user == null) return Result.error("请先以求职者身份登录");

        try {
            Resume resume;
            Object idObj = body.get("id");
            if (idObj != null) {
                // 更新已有简历 → 水平越权防护
                Long resumeId = Long.valueOf(idObj.toString());
                resume = resumeService.getResumeById(resumeId);
                if (!resume.getUserId().equals(user.getId())) {
                    return Result.error("无权修改他人简历");
                }
            } else {
                // 新建简历
                resume = new Resume();
                resume.setUserId(user.getId());
            }

            // 逐字段安全更新（仅更新 body 中包含的字段，避免 null 覆盖）
            if (body.get("realName") != null) resume.setRealName((String) body.get("realName"));
            if (body.get("gender") != null) resume.setGender((String) body.get("gender"));
            if (body.get("phone") != null) resume.setPhone((String) body.get("phone"));
            if (body.get("email") != null) resume.setEmail((String) body.get("email"));
            if (body.get("birthday") != null) resume.setBirthday((String) body.get("birthday"));
            if (body.get("education") != null) resume.setEducation((String) body.get("education"));
            if (body.get("school") != null) resume.setSchool((String) body.get("school"));
            if (body.get("major") != null) resume.setMajor((String) body.get("major"));
            if (body.get("workExperience") != null) resume.setWorkExperience((String) body.get("workExperience"));
            if (body.get("projectExperience") != null) resume.setProjectExperience((String) body.get("projectExperience"));
            if (body.get("skills") != null) resume.setSkills((String) body.get("skills"));
            if (body.get("selfIntroduction") != null) resume.setSelfIntroduction((String) body.get("selfIntroduction"));

            Resume saved = resumeService.saveResume(resume);
            return Result.success(buildResumeJson(saved));

        } catch (Exception e) {
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    // ============================================
    // 8. 通知列表
    // GET /api/seeker/notifications
    // ============================================
    @GetMapping("/api/seeker/notifications")
    @ResponseBody
    public Result<Map<String, Object>> notifications(HttpSession session) {
        User user = requireSeeker(session);
        if (user == null) return Result.error("请先以求职者身份登录");

        List<Notification> notifications = notificationService.getUserNotifications(user.getId());
        int unreadCount = notificationService.getUnreadCount(user.getId());

        List<Map<String, Object>> list = new ArrayList<>();
        for (Notification n : notifications) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", n.getId());
            item.put("type", n.getType());
            item.put("content", n.getContent());
            item.put("relatedId", n.getRelatedId());
            item.put("isRead", n.getIsRead());
            item.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
            list.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("notifications", list);
        data.put("unreadCount", unreadCount);

        return Result.success(data);
    }

    // ============================================
    // 9. 聊天会话列表（求职者端）
    // GET /api/seeker/conversations
    // ============================================
    @GetMapping("/api/seeker/conversations")
    @ResponseBody
    public Result<List<Map<String, Object>>> conversations(HttpSession session) {
        User user = requireSeeker(session);
        if (user == null) return Result.error("请先以求职者身份登录");

        List<Application> apps = applicationService.getApplicationsByUser(user.getId());

        // 批量加载职位和公司信息
        List<Long> jobIds = apps.stream()
                .map(Application::getJobId).distinct().collect(Collectors.toList());
        Map<Long, Job> jobMap = new HashMap<>();
        if (!jobIds.isEmpty()) {
            jobService.getJobsByIds(jobIds).forEach(j -> jobMap.put(j.getId(), j));
        }

        List<Long> companyIds = jobMap.values().stream()
                .map(Job::getCompanyId).distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyService.getCompanyMapByIds(companyIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Application app : apps) {
            Map<String, Object> conv = new LinkedHashMap<>();
            String conversationId = "app_" + app.getId();
            conv.put("applicationId", idObfuscator.encode(app.getId()));
            conv.put("conversationId", conversationId);
            conv.put("status", app.getStatus());
            conv.put("unread", messageService.getUnreadCount(conversationId, user.getId()));

            // 最后一条消息预览
            messageService.getLastMessage(conversationId).ifPresent(lastMsg -> {
                Map<String, Object> lastMessage = new LinkedHashMap<>();
                lastMessage.put("messageType", lastMsg.getMessageType());
                lastMessage.put("content", lastMsg.getContent());
                lastMessage.put("createdAt", lastMsg.getCreatedAt() != null
                        ? lastMsg.getCreatedAt().toString() : null);
                conv.put("lastMessage", lastMessage);
            });

            Job job = jobMap.get(app.getJobId());
            if (job != null) {
                conv.put("jobTitle", job.getTitle());
                Company company = companyMap.get(job.getCompanyId());
                conv.put("companyName", company != null ? company.getName() : "");
                // 对方信息（BOSS）
                if (company != null) {
                    conv.put("otherUserId", company.getBossId());
                    conv.put("otherUserName", company.getContactName() != null
                            ? company.getContactName() : "招聘方");
                }
            } else {
                conv.put("jobTitle", "职位已删除");
                conv.put("companyName", "");
            }
            result.add(conv);
        }

        // 按未读数和最新消息时间排序
        result.sort((a, b) -> {
            int unreadA = (int) a.getOrDefault("unread", 0);
            int unreadB = (int) b.getOrDefault("unread", 0);
            if (unreadA != unreadB) return Integer.compare(unreadB, unreadA);
            return 0;
        });

        return Result.success(result);
    }

    // ============================================
    // 10. 退出登录
    // POST /api/seeker/logout
    // ============================================
    @PostMapping("/api/seeker/logout")
    @ResponseBody
    public Result<?> logout(HttpSession session, jakarta.servlet.http.HttpServletResponse response,
                            jakarta.servlet.http.HttpServletRequest request) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            session.invalidate();
        }
        // 清除 JWT Cookie
        com.weib.util.CookieUtil.deleteJwtCookie(response, request);
        com.weib.util.CookieUtil.deleteRememberTokenCookie(response, request);
        return Result.success("已退出登录");
    }
}
