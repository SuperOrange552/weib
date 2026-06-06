package com.weib.controller;

import com.weib.entity.Company;
import com.weib.entity.Job;
import com.weib.entity.Resume;
import com.weib.entity.User;
import com.weib.service.CompanyService;
import com.weib.service.JobService;
import com.weib.service.ResumeService;
import com.weib.util.IdObfuscator;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ============================================
 * 【Controller】首页控制器 - 职位列表
 * ============================================
 * 
 * 职责：
 * - 显示首页职位列表
 * - 支持职位搜索和筛选
 * - 加载公司和Boss信息
 * 
 * ----------------------------------------
 * 【为什么需要这个Controller？】
 * ----------------------------------------
 * 
 * 用户访问网站第一步看到的就是首页
 * 首页需要展示：
 * 1. 职位列表（Job表）
 * 2. 公司信息（Company表，需要关联查询）
 * 3. 搜索功能（多个条件的组合查询）
 * 
 * ----------------------------------------
 * 【首页的访问流程】
 * ----------------------------------------
 * 
 * 用户访问 http://localhost:8080/
 * → GET /
 * → IndexController.index() 处理
 * → 查询职位列表、加载公司信息
 * → 返回 index.html 渲染
 */
@Controller
@RequiredArgsConstructor
public class IndexController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IndexController.class);

    /**
     * ----------------------------------------
     * 【依赖注入】Service 层
     * ----------------------------------------
     * 
     * @RequiredArgsConstructor + private final = 构造器注入
     * 
     * 这是依赖注入的第三种方式（最推荐）：
     * 1. @Autowired + @Component（字段注入，不推荐）
     * 2. @Autowired + 构造器（之前的方式）
     * 3. @RequiredArgsConstructor + private final（现在的推荐）
     * 
     * 【为什么推荐这种方式？】
     * - 代码更简洁（不需要写@Autowired）
     * - 保证依赖不为null（final字段必须在构造器初始化）
     * - 方便测试（可以手动new一个mock对象传入）
     * - 符合不可变对象原则
     * 
     * 【JobService 的职责】
     * - 提供职位列表查询
     * - 提供职位搜索
     * - 提供职位详情
     * 
     * 【CompanyService 的职责】
     * - 提供公司信息查询
     * - Boss用户获取自己的公司信息
     */
    private final JobService jobService;
    private final CompanyService companyService;
    private final ResumeService resumeService;
    private final IdObfuscator idObfuscator;

    /**
     * ========================================
     * 【首页】显示职位列表
     * ========================================
     * 
     * ----------------------------------------
     * 【@GetMapping("/")】根路径映射
     * ----------------------------------------
     * 
     * "/" 是网站的首页地址
     * 用户访问 http://localhost:8080/ 会触发这个方法
     * 
     * 【为什么用 "/" 而不是 "/index"？】
     * - "/" 是根路径，用户访问网站默认就是这个
     * - 更符合用户习惯
     * - SEO 友好
     * 
     * ----------------------------------------
     * 【@GetMapping(value = {"/", "/index"})】
     * ----------------------------------------
     * 
     * 这种写法可以让 "/" 和 "/index" 都能访问首页
     * 提供更好的兼容性
     * 
     * ----------------------------------------
     * 【参数说明】
     * ----------------------------------------
     * 
     * @param session    HTTP会话，获取当前登录用户
     * @param model      数据模型，向视图传递数据
     * @param keyword    搜索关键词（可选，URL参数）
     * @param city       城市筛选（可选）
     * @param education  学历筛选（可选）
     * 
     * 【@RequestParam 的细节】
     * - 如果参数名和变量名相同，可以省略 value
     * - required = false 表示可选参数
     * - 不传参数时值为 null
     * 
     * ----------------------------------------
     * 【首页数据准备】
     * ----------------------------------------
     * 
     * 1. 获取当前登录用户
     *    - 从 Session 中获取 "user" 属性
     *    - 用于判断用户角色，显示不同内容
     * 
     * 2. 搜索/筛选职位
     *    - 如果有关键词/城市/学历筛选条件
     *    - 调用 jobService.searchJobs() 查询
     *    - 否则查询所有活跃职位
     * 
     * 3. 加载公司信息
     *    - 职位表中只有 companyId
     *    - 需要根据 companyId 查询公司名称
     *    - 用 Map 缓存，避免 N+1 查询
     * 
     * 4. 判断用户角色
     *    - role = "boss" → 显示"Boss入口"
     *    - role = "seeker" → 显示"求职者入口"
     * 
     * ----------------------------------------
     * 【执行流程】
     * ----------------------------------------
     * 
     * 1. 用户访问 http://localhost:8080/
     * 2. Spring MVC 匹配到 @GetMapping("/")
     * 3. 执行 index() 方法
     * 4. 从 Session 获取当前用户
     * 5. 根据筛选条件查询职位列表
     * 6. 批量查询公司信息，构建 companyMap
     * 7. 将数据存入 Model
     * 8. 返回视图名 "index"
     * 9. Thymeleaf 渲染 index.html
     * 
     * ----------------------------------------
     * 【Thymeleaf 模板中的数据使用】
     * ----------------------------------------
     * 
     * Controller:
     *   model.addAttribute("jobs", jobs);
     *   model.addAttribute("companyMap", companyMap);
     *   model.addAttribute("user", user);
     * 
     * HTML (Thymeleaf):
     *   <div th:each="job : ${jobs}">
     *     <span th:text="${job.title}"></span>
     *     <span th:text="${companyMap[job.companyId].name}"></span>
     *   </div>
     *   <div th:if="${user != null}">
     *     欢迎, <span th:text="${user.username}"></span>
     *   </div>
     */
    @GetMapping(value = {"/", "/index"})
    public String index(HttpSession session,
                        Model model,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String city,
                        @RequestParam(required = false) String education,
                        @RequestParam(required = false) String experience,
                        @RequestParam(required = false) Integer salaryMin,
                        @RequestParam(required = false) Integer salaryMax,
                        @RequestParam(defaultValue = "newest") String sort,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "12") int size) {

        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);

        // 参数校验：防止负数页码和异常页大小
        if (page < 0) page = 0;
        if (size < 1) size = 12;
        if (size > 100) size = 100;

        // 搜索或获取所有活跃职位
        Page<Job> jobPage;
        boolean hasFilters = (keyword != null && !keyword.isBlank()) 
                || (city != null && !city.isBlank()) 
                || (education != null && !education.isBlank())
                || (experience != null && !experience.isBlank())
                || salaryMin != null 
                || salaryMax != null;
        if (hasFilters) {
            jobPage = jobService.searchJobsPaged(keyword, city, education, experience, salaryMin, salaryMax, sort, page, size);
        } else {
            jobPage = jobService.getActiveJobsPaged(page, size);
        }

        List<Job> jobs = jobPage.getContent();

        // 批量加载公司信息（避免 N+1 查询）
        List<Long> companyIds = jobs.stream()
                .map(Job::getCompanyId).distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyService.getCompanyMapByIds(companyIds);

        // 求职者个性化推荐（基于简历匹配，仅取最近50个活跃职位做匹配）
        if (user != null && "seeker".equals(user.getRole()) && !hasFilters && page == 0) {
            try {
                Resume resume = resumeService.getResumeByUserId(user.getId());
                if (resume != null) {
                    List<Job> candidateJobs = jobService.getRecentActiveJobs(50);
                    List<Job> recommended = rankJobsByResume(candidateJobs, resume);
                    Set<Long> existingIds = jobs.stream().map(Job::getId).collect(Collectors.toSet());
                    List<Job> topRecommendations = recommended.stream()
                            .filter(j -> !existingIds.contains(j.getId()))
                            .limit(6)
                            .collect(Collectors.toList());
                    if (!topRecommendations.isEmpty()) {
                        List<Long> recCompanyIds = topRecommendations.stream()
                                .map(Job::getCompanyId).distinct().collect(Collectors.toList());
                        Map<Long, Company> recCompanyMap = companyService.getCompanyMapByIds(recCompanyIds);
                        model.addAttribute("recommendedJobs", topRecommendations);
                        model.addAttribute("recCompanyMap", recCompanyMap);
                        Map<Long, String> recEncodedJobIds = new HashMap<>();
                        for (Job j : topRecommendations) recEncodedJobIds.put(j.getId(), idObfuscator.encode(j.getId()));
                        Map<Long, String> recEncodedCompanyIds = new HashMap<>();
                        for (Long cid : recCompanyIds) recEncodedCompanyIds.put(cid, idObfuscator.encode(cid));
                        model.addAttribute("recEncodedJobIds", recEncodedJobIds);
                        model.addAttribute("recEncodedCompanyIds", recEncodedCompanyIds);
                    }
                }
            } catch (Exception e) {
                log.warn("简历推荐计算失败, userId={}", user != null ? user.getId() : "null", e);
            }
        }

        model.addAttribute("jobs", jobs);
        model.addAttribute("companyMap", companyMap);

        // ID 混淆：防止 IDOR 攻击
        Map<Long, String> encodedJobIds = new HashMap<>();
        for (Job job : jobs) encodedJobIds.put(job.getId(), idObfuscator.encode(job.getId()));
        Map<Long, String> encodedCompanyIds = new HashMap<>();
        for (Long cid : companyIds) encodedCompanyIds.put(cid, idObfuscator.encode(cid));
        model.addAttribute("encodedJobIds", encodedJobIds);
        model.addAttribute("encodedCompanyIds", encodedCompanyIds);

        model.addAttribute("keyword", keyword);
        model.addAttribute("city", city);
        model.addAttribute("education", education);
        model.addAttribute("experience", experience);
        model.addAttribute("salaryMin", salaryMin);
        model.addAttribute("salaryMax", salaryMax);
        model.addAttribute("sort", sort);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", jobPage.getTotalPages());
        model.addAttribute("totalItems", jobPage.getTotalElements());
        model.addAttribute("pageSize", size);

        int startPage = Math.max(0, page - 2);
        int endPage = Math.min(jobPage.getTotalPages() - 1, page + 2);
        if (endPage <= startPage) { endPage = startPage; }
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("pageNumbers", IntStream.rangeClosed(startPage, endPage).boxed().collect(Collectors.toList()));

        return "index";
    }

    /**
     * 基于简历的职位推荐排名
     * 简单关键词匹配算法：技能、学历、经验匹配度评分
     */
    private List<Job> rankJobsByResume(List<Job> jobs, Resume resume) {
        return jobs.stream()
                .filter(j -> "active".equals(j.getStatus()))
                .map(j -> new AbstractMap.SimpleEntry<>(j, matchScore(j, resume)))
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Job, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int matchScore(Job job, Resume resume) {
        int score = 0;
        if (resume.getEducation() != null && resume.getEducation().equals(job.getEducation())) {
            score += 3;
        }
        if (resume.getSkills() != null && job.getTags() != null) {
            String[] skills = resume.getSkills().toLowerCase().split("[,\\s、]+");
            String[] tags = job.getTags().toLowerCase().split("[,\\s、]+");
            for (String skill : skills) {
                String s = skill.trim();
                if (s.isEmpty()) continue;
                for (String tag : tags) {
                    String t = tag.trim();
                    if (t.isEmpty()) continue;
                    if (s.contains(t) || t.contains(s)) {
                        score += 5;
                        break; // 每技能只计一次
                    }
                }
            }
        }
        return score;
    }

    /**
     * ========================================
     * 【职位详情页】
     * ========================================
     * 
     * 显示单个职位的完整信息
     * 包括职位描述、要求、公司信息等
     * 
     * ----------------------------------------
     * 【@GetMapping("/job/{id}")】路径变量
     * ----------------------------------------
     * 
     * {id} 是路径变量，表示职位的 ID
     * 
     * 【URL 示例】
     * - /job/1     → id = 1
     * - /job/100   → id = 100
     * 
     * 【@PathVariable】获取路径变量
     * 
     * @PathVariable Long id
     * - 从 URL 路径中提取 {id}
     * - 自动转换为 Long 类型
     * 
     * 【为什么不放在 URL 参数里？】
     * - /job/detail?id=1（?id=1 是 Query 参数）
     * - /job/1（/job/1 是 Path 参数）
     * - RESTful 风格推荐用 Path 参数
     * - 更清晰、更美观、更符合语义
     * 
     * ----------------------------------------
     * 【职位详情需要展示的信息】
     * ----------------------------------------
     * 
     * 1. 职位基本信息
     *    - 职位名称、薪资范围
     *    - 城市、地址、学历要求、工作经验
     *    - 职位标签
     * 
     * 2. 职位描述
     *    - 工作内容
     *    - 岗位要求
     * 
     * 3. 公司信息
     *    - 公司名称、logo
     *    - 行业、规模
     *    - 公司介绍
     *    - 联系方式
     * 
     * 4. 操作按钮
     *    - 投递简历（求职者可见）
     *    - 查看简历（HR可见）
     */
    @GetMapping("/job/{encodedId}")
    public String jobDetail(@PathVariable String encodedId,
                            HttpSession session,
                            Model model) {

        Long id = idObfuscator.decode(encodedId);
        if (id == null) return "redirect:/";

        // 获取当前用户
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);

        try {
            Job job = jobService.getJobById(id);
            // 禁止访问非活跃职位（已关闭/下线的职位不可直接通过URL访问）
            if (!"active".equals(job.getStatus())) {
                return "redirect:/";
            }
            model.addAttribute("job", job);
            
            // 查询公司信息
            Company company = companyService.getCompanyById(job.getCompanyId());
            model.addAttribute("company", company);
            model.addAttribute("encodedJobId", encodedId);
            model.addAttribute("encodedCompanyId", idObfuscator.encode(company.getId()));

            // ========================================
            // 【高德地图打点】自动地理编码 + 构建地图数据
            // ========================================
            if (company.getLongitude() == null || company.getLatitude() == null) {
                // 异步地理编码 + 持久化，不阻塞页面加载
                companyService.geocodeAndPersistAsync(company);
            }
            if (company.getLongitude() != null && company.getLatitude() != null) {
                model.addAttribute("companyLng", company.getLongitude());
                model.addAttribute("companyLat", company.getLatitude());
            }
            
            // 增加浏览量（异步更好，这里简化处理）
            jobService.incrementViewCount(id);
            
            // 判断用户是否已投递
            /**
             * 【业务逻辑判断】
             * 
             * 用户已登录且是求职者
             * → 检查是否已投递该职位
             * → 传递给前端，决定按钮显示
             * 
             * 【为什么在 Controller 里判断？】
             * - 这是简单的布尔判断
             * - 适合在 Controller 做
             * - 如果逻辑复杂，应该放到 Service
             */
            boolean hasApplied = false;
            if (user != null && "seeker".equals(user.getRole())) {
                hasApplied = jobService.hasApplied(id, user.getId());
            }
            model.addAttribute("hasApplied", hasApplied);
            
        } catch (Exception e) {
            // 职位不存在，跳转回首页
            return "redirect:/";
        }
        
        return "job-detail";
    }

    /**
     * 公司详情页
     */
    @GetMapping("/company/{encodedId}")
    public String companyDetail(@PathVariable String encodedId, HttpSession session, Model model) {
        Long id = idObfuscator.decode(encodedId);
        if (id == null) return "redirect:/";

        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);

        try {
            Company company = companyService.getCompanyById(id);
            model.addAttribute("company", company);
            model.addAttribute("encodedCompanyId", encodedId);
            List<Job> jobs = jobService.getJobsByCompanyId(id);
            model.addAttribute("jobs", jobs);
            Map<Long, String> encodedJobIds = new HashMap<>();
            for (Job job : jobs) encodedJobIds.put(job.getId(), idObfuscator.encode(job.getId()));
            model.addAttribute("encodedJobIds", encodedJobIds);
        } catch (Exception e) {
            return "redirect:/";
        }

        return "company-detail";
    }
}
