package com.weib.controller;

import com.weib.dto.Result;
import com.weib.entity.Application;
import com.weib.entity.Company;
import com.weib.entity.Job;
import com.weib.entity.Resume;
import com.weib.entity.User;
import com.weib.service.ApplicationService;
import com.weib.service.CompanyService;
import com.weib.service.JobService;
import com.weib.service.MapService;
import com.weib.service.NotificationService;
import com.weib.service.ResumeService;
import com.weib.repository.UserRepository;
import com.weib.util.IdObfuscator;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================
 * 【Controller】Boss控制器 - Boss端功能
 * ============================================
 * 
 * 职责：
 * - Boss入驻（创建公司）
 * - 发布/编辑/关闭职位
 * - 查看收到的投递
 * - 处理投递申请
 * 
 * ----------------------------------------
 * 【Boss是谁？】
 * ----------------------------------------
 * 
 * Boss = 招聘方（企业HR/老板）
 * 
 * 与求职者(Seeker)的区别：
 * - Seeker：找工作，投递简历
 * - Boss：发布职位，筛选简历
 * 
 * ----------------------------------------
 * 【用户角色】
 * ----------------------------------------
 * 
 * 系统通过 User.role 字段区分：
 * - role = "seeker" → 求职者
 * - role = "boss" → Boss
 * 
 * 【Boss的工作流程】
 * 1. 注册账号
 * 2. 完善公司信息（入驻）
 * 3. 发布职位
 * 4. 查看投递
 * 5. 处理申请（通过/拒绝）
 */
@Controller
@RequiredArgsConstructor
public class BossController {

    /**
     * ----------------------------------------
     * 【依赖注入】三个核心Service
     * ----------------------------------------
     * 
     * JobService：职位管理
     * - 创建职位
     * - 更新职位
     * - 关闭职位
     * 
     * CompanyService：公司管理
     * - 创建公司
     * - 更新公司信息
     * 
     * ApplicationService：投递管理
     * - 查看收到的投递
     * - 更新投递状态
     */
    private final JobService jobService;
    private final CompanyService companyService;
    private final ApplicationService applicationService;
    private final MapService mapService;
    private final NotificationService notificationService;
    private final ResumeService resumeService;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;

    // ==========================================
    // 【Boss首页】我的公司
    // ==========================================

    /**
     * ----------------------------------------
     * 【@GetMapping("/boss")】Boss个人中心
     * ----------------------------------------
     * 
     * Boss登录后看到的首页
     * 显示公司信息和统计数据
     * 
     * 【页面内容】
     * - 公司基本信息
     * - 在招职位数量
     * - 收到简历数量
     * - 待处理申请数量
     */
    @GetMapping("/boss")
    public String bossHome(HttpSession session, Model model) {
        
        // ========================================
        // 【登录检查】
        // ========================================
        
        /**
         * 【为什么要检查登录？】
         * 
         * Boss功能只有Boss用户才能访问
         * 需要确保：
         * 1. 用户已登录
         * 2. 用户是Boss角色
         */
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return "redirect:/login";
        }
        
        if (!"boss".equals(user.getRole())) {
            return "redirect:/";
        }
        
        model.addAttribute("user", user);
        
        // ========================================
        // 【检查是否已入驻】
        // ========================================
        
        /**
         * 【入驻检查】
         * 
         * Boss入驻 = 创建公司信息
         * 
         * 判断逻辑：
         * - 已入驻 → 显示公司信息
         * - 未入驻 → 跳转到入驻页面
         * 
         * 【为什么不直接创建公司？】
         * - 需要Boss手动填写信息
         * - 需要确认信息准确
         * - 这是重要的初始化步骤
         */
        try {
            Company company = companyService.getCompanyByBossId(user.getId());
            model.addAttribute("company", company);

            List<Job> jobs = jobService.getJobsByCompanyId(company.getId());
            int activeJobs = (int) jobs.stream().filter(j -> "active".equals(j.getStatus())).count();
            int closedJobs = jobs.size() - activeJobs;

            // 批量查询所有职位的投递（一次查询替代 N 次循环查询）
            List<Long> jobIds = jobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
            List<Application> allApps = jobIds.isEmpty() ? List.of() : applicationService.getApplicationsByJobIds(jobIds);

            // 构建 jobId -> job 映射
            Map<Long, Job> jobMap = jobs.stream().collect(java.util.stream.Collectors.toMap(Job::getId, j -> j));

            // 统计各项数据
            int totalApps = allApps.size();
            int pendingApps = (int) allApps.stream().filter(a -> "pending".equals(a.getStatus())).count();
            int viewedApps = (int) allApps.stream().filter(a -> "viewed".equals(a.getStatus())).count();
            int acceptedApps = (int) allApps.stream().filter(a -> "accepted".equals(a.getStatus())).count();

            model.addAttribute("jobCount", activeJobs);
            model.addAttribute("closedJobs", closedJobs);
            model.addAttribute("applicationCount", totalApps);
            model.addAttribute("pendingApps", pendingApps);
            model.addAttribute("viewedApps", viewedApps);
            model.addAttribute("acceptedApps", acceptedApps);

            Map<Long, String> encodedJobIds = new HashMap<>();
            for (Job j : jobs) encodedJobIds.put(j.getId(), idObfuscator.encode(j.getId()));
            model.addAttribute("encodedJobIds", encodedJobIds);

            // 最近投递（前5条），关联职位名称
            for (Application app : allApps) {
                Job job = jobMap.get(app.getJobId());
                if (job != null) app.setJobTitle(job.getTitle());
            }
            allApps.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            List<Application> recentApps = allApps.size() > 5 ? allApps.subList(0, 5) : allApps;
            model.addAttribute("recentApps", recentApps);

        } catch (Exception e) {
            // 未入驻
            model.addAttribute("needRegister", true);
        }
        
        return "boss-home";
    }

    // ==========================================
    // 【入驻】创建公司
    // ==========================================

    /**
     * ----------------------------------------
     * 【@GetMapping("/boss/register")】入驻页面
     * ----------------------------------------
     * 
     * Boss完善公司信息
     * 只有未入驻的Boss才能访问
     */
    @GetMapping("/boss/register")
    public String registerPage(HttpSession session, Model model) {
        
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return "redirect:/login";
        }
        
        if (!"boss".equals(user.getRole())) {
            return "redirect:/";
        }
        
        // 已入驻则跳转到Boss首页
        try {
            Company company = companyService.getCompanyByBossId(user.getId());
            return "redirect:/boss";
        } catch (Exception e) {
            // 未入驻，继续
        }
        
        model.addAttribute("user", user);
        return "boss-register";
    }

    /**
     * ----------------------------------------
     * 【@PostMapping("/boss/register")】提交入驻
     * ----------------------------------------
     * 
     * 保存公司信息
     * 
     * 【参数说明】
     * - name: 公司名称
     * - industry: 行业
     * - scale: 公司规模
     * - address: 地址
     * - description: 公司介绍
     */
    @PostMapping("/boss/register")
    public String doRegister(@RequestParam String name,
                             @RequestParam(required = false) String industry,
                             @RequestParam(required = false) String scale,
                             @RequestParam(required = false) String address,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String contactName,
                             @RequestParam(required = false) String contactPhone,
                             @RequestParam(required = false) String contactEmail,
                             @RequestParam(required = false) Double longitude,
                             @RequestParam(required = false) Double latitude,
                             HttpSession session,
                             Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        // 非Boss用户不能入驻
        if (!"boss".equals(user.getRole())) {
            return "redirect:/";
        }

        // 防止重复入驻
        if (companyService.existsByBossId(user.getId())) {
            return "redirect:/boss";
        }

        // 构建公司对象
        Company company = new Company();
        company.setName(name);
        company.setIndustry(industry);
        company.setScale(scale);
        company.setAddress(address);
        company.setDescription(description);
        company.setContactName(contactName);
        company.setContactPhone(contactPhone);
        company.setContactEmail(contactEmail);
        company.setBossId(user.getId());  // 关联Boss

        // 地图坐标：优先手动选择，地址自动地理编码改为异步
        if (longitude != null && latitude != null) {
            company.setLongitude(longitude);
            company.setLatitude(latitude);
        }

        // 保存
        try {
            company = companyService.createCompany(company);
            // 异步地理编码，不阻塞页面响应
            if ((longitude == null || latitude == null) && address != null && !address.isEmpty()) {
                companyService.geocodeAndPersistAsync(company);
            }
            return "redirect:/boss";
        } catch (Exception e) {
            model.addAttribute("error", "入驻失败：" + e.getMessage());
            model.addAttribute("user", user);
            return "boss-register";
        }
    }

    // ==========================================
    // 【职位管理】
    // ==========================================

    /**
     * ----------------------------------------
     * 【@GetMapping("/boss/jobs")】职位管理页
     * ----------------------------------------
     * 
     * Boss查看自己发布的所有职位
     */
    @GetMapping("/boss/jobs")
    public String manageJobs(HttpSession session, Model model) {
        
        User user = (User) session.getAttribute("user");
        
        if (user == null || !"boss".equals(user.getRole())) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        
        // 获取公司
        try {
            Company company = companyService.getCompanyByBossId(user.getId());
            
            // 获取职位列表
            List<Job> jobs = jobService.getJobsByCompanyId(company.getId());
            
            // 批量统计每个职位的投递数量（避免 N+1 查询）
            List<Long> jobIds = jobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
            List<Application> allApps = jobIds.isEmpty() ? List.of() : applicationService.getApplicationsByJobIds(jobIds);
            Map<Long, Long> appCountMap = allApps.stream()
                    .collect(java.util.stream.Collectors.groupingBy(Application::getJobId, java.util.stream.Collectors.counting()));

            model.addAttribute("company", company);
            model.addAttribute("jobs", jobs);
            model.addAttribute("appCountMap", appCountMap);
            Map<Long, String> encodedJobIds = new HashMap<>();
            for (Job j : jobs) encodedJobIds.put(j.getId(), idObfuscator.encode(j.getId()));
            model.addAttribute("encodedJobIds", encodedJobIds);
            
        } catch (Exception e) {
            return "redirect:/boss/register";
        }
        
        return "boss-jobs";
    }

    /**
     * ----------------------------------------
     * 【@GetMapping("/boss/job/new")】发布职位页
     * ----------------------------------------
     */
    @GetMapping("/boss/job/new")
    public String newJob(HttpSession session, Model model) {
        
        User user = (User) session.getAttribute("user");
        
        if (user == null || !"boss".equals(user.getRole())) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("job", new Job());
        model.addAttribute("isEdit", false);
        
        return "boss-job-edit";
    }

    /**
     * ----------------------------------------
     * 【@GetMapping("/boss/job/edit/{id}")】编辑职位页
     * ----------------------------------------
     * 
     * @PathVariable Long id  →  从URL路径获取职位ID
     */
    @GetMapping("/boss/job/edit/{encodedId}")
    public String editJob(@PathVariable String encodedId,
                          HttpSession session,
                          Model model) {

        Long id = idObfuscator.decode(encodedId);
        if (id == null) return "redirect:/boss/jobs";

        User user = (User) session.getAttribute("user");

        if (user == null || !"boss".equals(user.getRole())) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);

        try {
            Job job = jobService.getJobById(id);
            
            // 检查权限（只能编辑自己公司的职位）
            Company company = companyService.getCompanyByBossId(user.getId());
            if (!job.getCompanyId().equals(company.getId())) {
                return "redirect:/boss/jobs";
            }
            
            model.addAttribute("job", job);
            model.addAttribute("isEdit", true);
            
        } catch (Exception e) {
            return "redirect:/boss/jobs";
        }
        
        return "boss-job-edit";
    }

    /**
     * ----------------------------------------
     * 【@PostMapping("/boss/job/save")】保存职位
     * ----------------------------------------
     * 
     * 新建和编辑共用一个方法
     * 通过是否有 id 参数判断
     */
    @PostMapping("/boss/job/save")
    public String saveJob(@RequestParam(required = false) Long id,
                          @RequestParam String title,
                          @RequestParam(required = false) Integer salaryMin,
                          @RequestParam(required = false) Integer salaryMax,
                          @RequestParam(required = false) String city,
                          @RequestParam(required = false) String address,
                          @RequestParam String education,
                          @RequestParam String experience,
                          @RequestParam String description,
                          @RequestParam(required = false) String requirements,
                          @RequestParam(required = false) String tags,
                          HttpSession session,
                          Model model) {
        
        User user = (User) session.getAttribute("user");
        
        if (user == null || !"boss".equals(user.getRole())) {
            return "redirect:/login";
        }
        
        // 获取公司
        Company company;
        try {
            company = companyService.getCompanyByBossId(user.getId());
        } catch (Exception e) {
            return "redirect:/boss/register";
        }
        
        // 构建职位对象
        Job job = new Job();
        
        // 如果是编辑，先查询
        if (id != null) {
            try {
                job = jobService.getJobById(id);
                // 检查权限
                if (!job.getCompanyId().equals(company.getId())) {
                    return "redirect:/boss/jobs";
                }
            } catch (Exception e) {
                return "redirect:/boss/jobs";
            }
        }
        
        // 设置职位信息
        job.setCompanyId(company.getId());
        job.setTitle(title);
        if (salaryMin != null && salaryMax != null && salaryMin > salaryMax) {
            model.addAttribute("error", "最低薪资不能高于最高薪资");
            model.addAttribute("user", user);
            model.addAttribute("job", job);
            model.addAttribute("isEdit", id != null);
            return "boss-job-edit";
        }
        job.setSalaryMin(salaryMin);
        job.setSalaryMax(salaryMax);
        job.setCity(city);
        job.setAddress(address);
        job.setEducation(education);
        job.setExperience(experience);
        job.setDescription(description);
        job.setRequirements(requirements);
        job.setTags(tags);
        if (id == null) {
            job.setStatus("active");
        }
        // 编辑时保留原状态，不自动改为active

        // 保存
        try {
            if (id == null) {
                jobService.createJob(job);
            } else {
                jobService.updateJob(job);
            }
            return "redirect:/boss/jobs";
        } catch (Exception e) {
            model.addAttribute("error", "保存失败：" + e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("job", job);
            model.addAttribute("isEdit", id != null);
            return "boss-job-edit";
        }
    }

    /**
     * ----------------------------------------
     * 【@PostMapping("/boss/job/delete/{id}")】删除职位
     * ----------------------------------------
     * 
     * 软删除：设置为 inactive 状态
     */
    @PostMapping("/boss/job/delete/{encodedId}")
    public String deleteJob(@PathVariable String encodedId,
                             HttpSession session) {

        Long id = idObfuscator.decode(encodedId);
        if (id == null) return "redirect:/boss/jobs";

        User user = (User) session.getAttribute("user");

        if (user == null || !"boss".equals(user.getRole())) {
            return "redirect:/login";
        }

        try {
            Job job = jobService.getJobById(id);
            
            // 检查权限
            Company company = companyService.getCompanyByBossId(user.getId());
            if (!job.getCompanyId().equals(company.getId())) {
                return "redirect:/boss/jobs";
            }
            
            // 软删除：设置为关闭状态
            job.setStatus("closed");
            jobService.updateJob(job);
            
        } catch (Exception e) {
            // 忽略错误
        }
        
        return "redirect:/boss/jobs";
    }

    // ==========================================
    // 【投递管理】
    // ==========================================

    /**
     * ----------------------------------------
     * 【@GetMapping("/boss/applications")】投递列表
     * ----------------------------------------
     * 
     * Boss查看收到的所有投递
     */
    @GetMapping("/boss/applications")
    public String applications(HttpSession session, Model model) {
        
        User user = (User) session.getAttribute("user");
        
        if (user == null || !"boss".equals(user.getRole())) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        
        // 获取公司
        try {
            Company company = companyService.getCompanyByBossId(user.getId());
            model.addAttribute("company", company);

            List<Job> jobs = jobService.getJobsByCompanyId(company.getId());

            // 批量查询所有职位的投递（一次查询替代 N 次循环查询）
            List<Long> jobIds = jobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
            List<Application> allApplications = jobIds.isEmpty() ? new java.util.ArrayList<>()
                    : new java.util.ArrayList<>(applicationService.getApplicationsByJobIds(jobIds));

            // 构建 jobId -> jobTitle 映射
            Map<Long, String> jobTitleMap = jobs.stream()
                    .collect(java.util.stream.Collectors.toMap(Job::getId, Job::getTitle));
            for (Application app : allApplications) {
                app.setJobTitle(jobTitleMap.getOrDefault(app.getJobId(), "职位已删除"));
            }

            // 按时间降序排列
            allApplications.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

            // 批量加载求职者信息（一次查询替代 N 次循环查询）
            List<Long> seekerIds = allApplications.stream()
                    .map(Application::getUserId).distinct().collect(java.util.stream.Collectors.toList());
            Map<Long, User> seekerMap = new HashMap<>();
            if (!seekerIds.isEmpty()) {
                List<User> seekers = userRepository.findAllById(seekerIds);
                for (User s : seekers) seekerMap.put(s.getId(), s);
            }

            // 批量加载简历（避免 N+1）
            Map<Long, Resume> resumeMap = resumeService.getResumeMapByUserIds(seekerIds);

            Map<Long, String> seekerNames = new HashMap<>();
            Map<Long, String> seekerResumeSummary = new HashMap<>();
            Map<Long, Long> seekerResumeIds = new HashMap<>();
            for (Application app : allApplications) {
                User seeker = seekerMap.get(app.getUserId());
                if (seeker != null) {
                    seekerNames.put(app.getUserId(), seeker.getNickname() != null ? seeker.getNickname() : seeker.getUsername());
                } else {
                    seekerNames.putIfAbsent(app.getUserId(), "用户" + app.getUserId());
                }
                Resume resume = resumeMap.get(app.getUserId());
                if (resume != null) {
                    seekerResumeIds.put(app.getUserId(), resume.getId());
                    seekerResumeSummary.put(app.getUserId(), buildResumeSummary(resume));
                }
            }

            model.addAttribute("seekerNames", seekerNames);
            model.addAttribute("seekerResumeSummary", seekerResumeSummary);
            model.addAttribute("seekerResumeIds", seekerResumeIds);
            model.addAttribute("applications", allApplications);
            Map<Long, String> encodedApplicationIds = new HashMap<>();
            Map<Long, String> encodedSeekerIds = new HashMap<>();
            for (Application a : allApplications) {
                encodedApplicationIds.put(a.getId(), idObfuscator.encode(a.getId()));
                encodedSeekerIds.put(a.getUserId(), idObfuscator.encode(a.getUserId()));
            }
            model.addAttribute("encodedApplicationIds", encodedApplicationIds);
            model.addAttribute("encodedSeekerIds", encodedSeekerIds);

        } catch (Exception e) {
            return "redirect:/boss/register";
        }

        return "boss-applications";
    }

    private String buildResumeSummary(Resume resume) {
        StringBuilder sb = new StringBuilder();
        if (resume.getRealName() != null) sb.append("姓名：").append(resume.getRealName()).append("\n");
        if (resume.getEducation() != null) sb.append("学历：").append(resume.getEducation()).append("\n");
        if (resume.getSchool() != null) sb.append("学校：").append(resume.getSchool()).append("\n");
        if (resume.getMajor() != null) sb.append("专业：").append(resume.getMajor()).append("\n");
        if (resume.getSkills() != null) sb.append("技能：").append(resume.getSkills()).append("\n");
        if (resume.getWorkExperience() != null && resume.getWorkExperience().length() > 100) {
            sb.append("工作经历：").append(resume.getWorkExperience(), 0, 100).append("...\n");
        } else if (resume.getWorkExperience() != null) {
            sb.append("工作经历：").append(resume.getWorkExperience()).append("\n");
        }
        return sb.toString().trim();
    }

    @GetMapping("/boss/resume/{encodedUserId}")
    @ResponseBody
    public Result<Map<String, Object>> viewResume(@PathVariable String encodedUserId, HttpSession session) {
        Long userId = idObfuscator.decode(encodedUserId);
        if (userId == null) return Result.error("参数无效");

        User user = (User) session.getAttribute("user");
        if (user == null || !"boss".equals(user.getRole())) {
            return Result.error("请先登录");
        }
        try {
            // 验证此求职者是否投递了当前Boss公司的职位（批量查询避免N+1）
            Company company = companyService.getCompanyByBossId(user.getId());
            List<Job> jobs = jobService.getJobsByCompanyId(company.getId());
            List<Long> companyJobIds = jobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
            boolean hasApplied = applicationService.hasAppliedToAny(companyJobIds, userId);
            if (!hasApplied) {
                return Result.error(403, "无权查看此简历");
            }

            Resume resume = resumeService.getResumeByUserId(userId);
            User seeker = userRepository.findById(userId).orElse(null);
            Map<String, Object> data = new HashMap<>();
            data.put("resume", resume);
            data.put("seekerName", seeker != null ? (seeker.getNickname() != null ? seeker.getNickname() : seeker.getUsername()) : "未知");
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("简历不存在");
        }
    }

    /**
     * ----------------------------------------
     * 【@PostMapping("/boss/application/{id}/status")】
     * 更新投递状态
     * ----------------------------------------
     */
    @PostMapping("/boss/application/{encodedId}/status")
    @ResponseBody
    public Result<?> updateApplicationStatus(@PathVariable String encodedId,
                                              @RequestParam String status,
                                              @RequestParam(required = false) String bossNote,
                                              HttpSession session) {

        Long id = idObfuscator.decode(encodedId);
        if (id == null) return Result.error("参数无效");

        User user = (User) session.getAttribute("user");

        if (user == null || !"boss".equals(user.getRole())) {
            return Result.error("请先登录");
        }

        if (!List.of("viewed", "accepted", "rejected").contains(status)) {
            return Result.error("无效的投递状态: " + status);
        }

        try {
            Application app = applicationService.getApplicationById(id);
            Job job = jobService.getJobById(app.getJobId());
            Company company = companyService.getCompanyByBossId(user.getId());

            if (!job.getCompanyId().equals(company.getId())) {
                return Result.error(403, "无权操作此投递记录");
            }

            applicationService.updateStatus(id, status, bossNote);

            // 发送通知给求职者
            String statusText = switch (status) {
                case "viewed" -> "已被HR查看";
                case "accepted" -> "已通过筛选，等待进一步联系";
                case "rejected" -> "暂不匹配";
                default -> "状态更新为: " + status;
            };
            String notifContent = "您的投递【" + job.getTitle() + "】" + statusText;
            if (bossNote != null && !bossNote.isBlank()) {
                notifContent += "。Boss留言：" + bossNote;
            }
            notificationService.createNotification(app.getUserId(), "application_update", notifContent, app.getJobId());

            return Result.success("状态更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // ==========================================
    // 公司编辑
    // ==========================================

    @GetMapping("/boss/company/edit")
    public String editCompany(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"boss".equals(user.getRole())) return "redirect:/login";
        try {
            Company company = companyService.getCompanyByBossId(user.getId());
            model.addAttribute("company", company);
        } catch (Exception e) {
            return "redirect:/boss/register";
        }
        model.addAttribute("user", user);
        return "boss-company-edit";
    }

    @PostMapping("/boss/company/edit")
    public String updateCompany(@RequestParam(required = false) String industry,
                                @RequestParam(required = false) String scale,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String contactName,
                                @RequestParam(required = false) String contactPhone,
                                @RequestParam(required = false) String contactEmail,
                                @RequestParam(required = false) Double longitude,
                                @RequestParam(required = false) Double latitude,
                                HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"boss".equals(user.getRole())) return "redirect:/login";

        try {
            Company company = companyService.getCompanyByBossId(user.getId());
            if (industry != null) company.setIndustry(industry);
            if (scale != null) company.setScale(scale);
            if (address != null) company.setAddress(address);
            if (description != null) company.setDescription(description);
            if (contactName != null) company.setContactName(contactName);
            if (contactPhone != null) company.setContactPhone(contactPhone);
            if (contactEmail != null) company.setContactEmail(contactEmail);
            if (longitude != null && latitude != null) {
                company.setLongitude(longitude);
                company.setLatitude(latitude);
            } else if (address != null && !address.isEmpty() && (company.getLongitude() == null || company.getLatitude() == null)) {
                double[] coords = mapService.geocode(address, null);
                if (coords != null) {
                    company.setLongitude(coords[0]);
                    company.setLatitude(coords[1]);
                }
            }
            companyService.updateCompany(company);
            model.addAttribute("success", "公司信息已更新");
            model.addAttribute("company", company);
        } catch (Exception e) {
            model.addAttribute("error", "更新失败：" + e.getMessage());
        }
        model.addAttribute("user", user);
        return "boss-company-edit";
    }
}
