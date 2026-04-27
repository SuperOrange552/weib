package com.weib.controller;

import com.weib.entity.Application;
import com.weib.entity.Company;
import com.weib.entity.Job;
import com.weib.entity.User;
import com.weib.service.ApplicationService;
import com.weib.service.CompanyService;
import com.weib.service.JobService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            
            // 统计数据
            List<Job> jobs = jobService.getJobsByCompanyId(company.getId());
            model.addAttribute("jobCount", jobs.size());
            
            // 统计投递数量
            int applicationCount = 0;
            for (Job job : jobs) {
                List<Application> apps = applicationService.getApplicationsByJob(job.getId());
                applicationCount += apps.size();
            }
            model.addAttribute("applicationCount", applicationCount);
            
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
                             HttpSession session,
                             Model model) {
        
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return "redirect:/login";
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
        
        // 保存
        try {
            companyService.createCompany(company);
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
            
            // 统计每个职位的投递数量
            /**
             * 【关联查询】
             * 
             * 职位和投递的关系：
             * - 一个职位可以有多个投递
             * - 需要统计每个职位的投递数
             */
            for (Job job : jobs) {
                List<Application> apps = applicationService.getApplicationsByJob(job.getId());
                job.setViewCount(apps.size());  // 复用viewCount字段存储投递数
            }
            
            model.addAttribute("company", company);
            model.addAttribute("jobs", jobs);
            
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
    @GetMapping("/boss/job/edit/{id}")
    public String editJob(@PathVariable Long id,
                          HttpSession session,
                          Model model) {
        
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
        job.setSalaryMin(salaryMin);
        job.setSalaryMax(salaryMax);
        job.setCity(city);
        job.setAddress(address);
        job.setEducation(education);
        job.setExperience(experience);
        job.setDescription(description);
        job.setRequirements(requirements);
        job.setTags(tags);
        job.setStatus("active");
        
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
    @PostMapping("/boss/job/delete/{id}")
    public String deleteJob(@PathVariable Long id,
                             HttpSession session) {
        
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
            
            // 获取所有职位的投递
            List<Job> jobs = jobService.getJobsByCompanyId(company.getId());
            
            // 收集所有投递
            /**
             * 【数据聚合】
             * 
             * 投递是按职位分的
             * Boss想看所有投递
             * 需要把多个职位的投递合并
             */
            java.util.ArrayList<Application> allApplications = new java.util.ArrayList<>();
            java.util.ArrayList<Job> jobList = new java.util.ArrayList<>();
            
            for (Job job : jobs) {
                List<Application> apps = applicationService.getApplicationsByJob(job.getId());
                allApplications.addAll(apps);
                
                // 关联职位信息到投递记录
                for (Application app : apps) {
                    app.setJobTitle(job.getTitle());  // 临时字段
                }
            }
            
            // 按时间降序排列
            allApplications.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            
            model.addAttribute("applications", allApplications);
            
        } catch (Exception e) {
            return "redirect:/boss/register";
        }
        
        return "boss-applications";
    }

    /**
     * ----------------------------------------
     * 【@PostMapping("/boss/application/{id}/status")】
     * 更新投递状态
     * ----------------------------------------
     */
    @PostMapping("/boss/application/{id}/status")
    @ResponseBody
    public Result<?> updateApplicationStatus(@PathVariable Long id,
                                              @RequestParam String status,
                                              HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        
        if (user == null || !"boss".equals(user.getRole())) {
            return Result.error("请先登录");
        }
        
        try {
            applicationService.updateStatus(id, status, null);
            return Result.success("状态更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
