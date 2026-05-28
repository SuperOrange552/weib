package com.weib.controller;

import com.weib.entity.Application;
import com.weib.entity.Company;
import com.weib.entity.FavoriteJob;
import com.weib.entity.Job;
import com.weib.entity.User;
import com.weib.service.ApplicationService;
import com.weib.service.CompanyService;
import com.weib.service.FavoriteJobService;
import com.weib.service.JobService;
import com.weib.dto.Result;
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
 * 【Controller】职位控制器 - 职位投递
 * ============================================
 * 
 * 职责：
 * - 处理职位投递
 * - 查看投递状态
 * 
 * ----------------------------------------
 * 【为什么单独一个 Controller？】
 * ----------------------------------------
 * 
 * 职位相关的操作很多：
 * - 求职者：投递职位、查看投递记录
 * - Boss：发布职位、管理职位、查看投递
 * 
 * 为了代码清晰，按功能模块拆分：
 * - IndexController：首页、职位列表、职位详情
 * - JobController：职位投递相关
 * - BossController：Boss 相关操作
 * - ResumeController：简历相关
 * 
 * 【Controller 拆分原则】
 * - 按功能模块拆分（不是按实体拆分）
 * - 每个 Controller 负责一个完整的功能
 * - 相关的操作放在一起
 */
@Controller
@RequiredArgsConstructor
public class JobController {

    /**
     * ----------------------------------------
     * 【依赖注入】
     * ----------------------------------------
     * 
     * JobService：职位业务逻辑
     * - 投递职位前检查
     * - 判断是否已投递
     * 
     * ApplicationService：投递业务逻辑
     * - 执行投递操作
     * - 查询投递记录
     */
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final FavoriteJobService favoriteJobService;
    private final CompanyService companyService;
    private final IdObfuscator idObfuscator;

    /**
     * ========================================
     * 【投递职位】POST 请求
     * ========================================
     * 
     * ----------------------------------------
     * 【@PostMapping("/job/{id}/apply")】
     * ----------------------------------------
     * 
     * 投递职位的接口
     * 
     * 【RESTful 设计】
     * - POST   /job/1/apply   → 投递职位1
     * - DELETE /job/1/apply   → 取消投递职位1
     * 
     * 【为什么用 POST 而不是 GET？】
     * - 投递是修改数据的操作
     * - GET 请求不应该有副作用
     * - POST 更安全，参数不在 URL 中
     * 
     * ----------------------------------------
     * 【前置检查】
     * ----------------------------------------
     * 
     * 投递前需要检查：
     * 1. 用户是否登录 → 未登录返回错误
     * 2. 用户是否是求职者 → Boss 不能投递
     * 3. 简历是否完善 → 没简历不能投递
     * 4. 是否已投递 → 不能重复投递
     * 
     * ----------------------------------------
     * 【投递流程】
     * ----------------------------------------
     * 
     * 1. 获取当前用户
     * 2. 验证用户身份（求职者）
     * 3. 调用 ApplicationService.apply()
     * 4. 创建投递记录
     * 5. 返回结果
     */
    @PostMapping("/job/{encodedId}/apply")
    @ResponseBody
    public Result<?> apply(@PathVariable String encodedId, HttpSession session) {

        Long id = idObfuscator.decode(encodedId);
        if (id == null) return Result.error("参数无效");

        User user = (User) session.getAttribute("user");
        
        // ========================================
        // 第二步：登录检查
        // ========================================
        
        /**
         * 【未登录检查】
         * 
         * 如果用户未登录（user == null）
         * 返回错误信息，前端弹窗提示登录
         * 
         * 【为什么不直接跳转到登录页？】
         * - 这是 AJAX 请求
         * - AJAX 请求不能直接跳转页面
         * - 返回 JSON，前端根据 code 决定行为
         * 
         * 【Result<?> 的设计】
         * - Result<Integer> 表示成功时返回 Integer
         * - Result<?> 表示可以是任意类型
         * - success() 方法返回 Result.success(null)
         */
        if (user == null) {
            return Result.error("请先登录");
        }
        
        // ========================================
        // 第三步：角色检查
        // ========================================
        
        /**
         * 【角色检查】
         * 
         * 系统有两种用户：
         * - seeker（求职者）：可以投递职位
         * - boss（Boss）：不能投递，只能发布
         * 
         * 【为什么 Boss 不能投递？】
         * - Boss 是招聘方，不是求职方
         * - Boss 的角色是发布职位、查看投递
         */
        if (!"seeker".equals(user.getRole())) {
            return Result.error("只有求职者才能投递简历");
        }
        
        // ========================================
        // 第四步：执行投递
        // ========================================
        
        /**
         * 【try-catch 处理业务异常】
         * 
         * ApplicationService.apply() 可能抛出异常：
         * - "您已投递过该职位" → 已投递
         * - "请先完善简历" → 没简历
         * - "职位不存在" → 职位被删了
         * 
         * 【为什么用 RuntimeException？】
         * - 业务异常用 RuntimeException
         * - 不用检查异常（不用 try-catch 或 throws）
         * - Spring 会统一处理
         */
        try {
            // 执行投递
            Application application = applicationService.apply(id, user.getId());
            
            // 投递成功
            return Result.success("投递成功！");
            
        } catch (RuntimeException e) {
            // 投递失败，返回错误信息
            return Result.error(e.getMessage());
        }
    }

    /**
     * ========================================
     * 【我的投递】查看投递记录
     * ========================================
     * 
     * 求职者查看自己投递了哪些职位
     * 
     * ----------------------------------------
     * 【@GetMapping("/my/applications")】
     * ----------------------------------------
     * 
     * 这是求职者的个人中心功能
     * 显示自己投递过的所有职位及状态
     * 
     * ----------------------------------------
     * 【需要展示的信息】
     * ----------------------------------------
     * 
     * 1. 投递记录列表
     *    - 职位名称
     *    - 公司名称
     *    - 投递时间
     *    - 当前状态（pending/viewed/accepted/rejected）
     * 
     * 2. 状态说明
     *    - pending：待处理（黄色）
     *    - viewed：已查看（蓝色）
     *    - accepted：已接收（绿色）
     *    - rejected：已拒绝（红色）
     * 
     * ----------------------------------------
     * 【数据查询】
     * ----------------------------------------
     * 
     * 1. 根据 userId 查询投递记录
     * 2. 根据 jobId 查询职位信息
     * 3. 根据 companyId 查询公司信息
     */
    @GetMapping("/my/applications")
    public String myApplications(HttpSession session, Model model) {
        
        // 获取当前用户
        User user = (User) session.getAttribute("user");
        
        // 未登录跳转登录页
        if (user == null) {
            return "redirect:/login";
        }
        
        // 非求职者不能访问
        if (!"seeker".equals(user.getRole())) {
            return "redirect:/";
        }
        
        // 查询投递记录
        /**
         * 【ApplicationService 查询方法】
         * 
         * getApplicationsByUser(userId)
         * - 根据用户ID查询所有投递记录
         * - 返回 List<Application>
         * - 按投递时间降序排列
         */
        List<Application> applications = applicationService.getApplicationsByUser(user.getId());

        // 批量加载职位信息（避免 N+1 查询）
        List<Long> jobIds = applications.stream().map(Application::getJobId).distinct().collect(java.util.stream.Collectors.toList());
        Map<Long, Job> jobMap = new HashMap<>();
        if (!jobIds.isEmpty()) {
            List<Job> jobs = jobService.getJobsByIds(jobIds);
            for (Job job : jobs) jobMap.put(job.getId(), job);
        }
        for (Application app : applications) {
            Job job = jobMap.get(app.getJobId());
            app.setJobTitle(job != null ? job.getTitle() : "职位已删除");
        }
        
        // 存入模型
        model.addAttribute("user", user);
        model.addAttribute("applications", applications);

        Map<Long, String> encodedApplicationIds = new HashMap<>();
        for (Application app : applications) encodedApplicationIds.put(app.getId(), idObfuscator.encode(app.getId()));
        model.addAttribute("encodedApplicationIds", encodedApplicationIds);

        return "my-applications";
    }

    @PostMapping("/job/{encodedId}/favorite")
    @ResponseBody
    public Result<?> toggleFavorite(@PathVariable String encodedId, HttpSession session) {
        Long id = idObfuscator.decode(encodedId);
        if (id == null) return Result.error("参数无效");

        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error("请先登录");
        if (!"seeker".equals(user.getRole())) return Result.error("只限求职者");

        favoriteJobService.toggleFavorite(id, user.getId());
        boolean isFav = favoriteJobService.isFavorited(id, user.getId());
        return Result.success(Map.of("favorited", isFav));
    }

    @GetMapping("/my/favorites")
    public String myFavorites(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!"seeker".equals(user.getRole())) return "redirect:/";

        List<FavoriteJob> favorites = favoriteJobService.getUserFavorites(user.getId());
        List<Long> favJobIds = favorites.stream().map(FavoriteJob::getJobId).distinct().collect(java.util.stream.Collectors.toList());
        Map<Long, Job> jobMap = new HashMap<>();
        if (!favJobIds.isEmpty()) {
            for (Job j : jobService.getJobsByIds(favJobIds)) {
                jobMap.put(j.getId(), j);
            }
        }
        List<Job> jobs = new java.util.ArrayList<>();
        for (FavoriteJob fav : favorites) {
            Job job = jobMap.get(fav.getJobId());
            if (job != null && "active".equals(job.getStatus())) {
                jobs.add(job);
            }
        }
        // 批量加载公司信息
        List<Long> companyIds = jobs.stream().map(Job::getCompanyId).distinct().collect(java.util.stream.Collectors.toList());
        Map<Long, Company> companyMap = companyService.getCompanyMapByIds(companyIds);

        model.addAttribute("user", user);
        model.addAttribute("jobs", jobs);
        model.addAttribute("companyMap", companyMap);

        Map<Long, String> encodedJobIds = new HashMap<>();
        for (Job j : jobs) encodedJobIds.put(j.getId(), idObfuscator.encode(j.getId()));
        Map<Long, String> encodedCompanyIds = new HashMap<>();
        for (Long cid : companyIds) encodedCompanyIds.put(cid, idObfuscator.encode(cid));
        model.addAttribute("encodedJobIds", encodedJobIds);
        model.addAttribute("encodedCompanyIds", encodedCompanyIds);

        return "my-favorites";
    }
}
