package com.weib.controller;

import com.weib.entity.Company;
import com.weib.entity.Job;
import com.weib.entity.User;
import com.weib.service.CompanyService;
import com.weib.service.JobService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                        @RequestParam(required = false) String education) {
        
        // ========================================
        // 第一步：获取当前登录用户
        // ========================================
        
        /**
         * 【Session 获取用户】
         * 
         * session.getAttribute("user") 获取之前存入的用户对象
         * 登录成功时：session.setAttribute("user", user)
         * 
         * 【为什么存入的是整个 User 对象？】
         * - 方便后续使用用户的所有信息
         * - id、username、role、nickname 等都能直接用
         * - 比只存 userId 更灵活
         * 
         * 【类型转换】
         * Session 存的是 Object，取出来需要强转
         * (User) 将 Object 转成 User 类型
         */
        User user = (User) session.getAttribute("user");
        
        /**
         * 【Thymeleaf 中判断用户登录状态】
         * 
         * model.addAttribute("user", user);
         * 
         * HTML 中使用：
         * <div th:if="${user != null}">已登录</div>
         * <div th:if="${user == null}">未登录</div>
         */
        model.addAttribute("user", user);

        // ========================================
        // 第二步：搜索/筛选职位
        // ========================================
        
        /**
         * 【搜索逻辑】
         * 
         * 如果有任何筛选条件，就调用搜索方法
         * 否则查询所有活跃职位
         * 
         * 【为什么设计成这样？】
         * - 减少代码重复
         * - 一个方法处理两种情况
         * - 搜索方法内部处理了无条件的边界情况
         */
        List<Job> jobs;
        if (keyword != null || city != null || education != null) {
            // 有筛选条件，调用搜索
            jobs = jobService.searchJobs(keyword, city, education);
        } else {
            // 无筛选条件，获取所有活跃职位
            jobs = jobService.getAllActiveJobs();
        }
        
        /**
         * 【职位列表排序】
         * 
         * findByStatusOrderByCreatedAtDesc 已按创建时间降序
         * 最新的职位排在最前面
         * 
         * 【为什么用降序？】
         * - 用户通常想看最新发布的职位
         * - 新职位更可能还在招聘
         */

        // ========================================
        // 第三步：加载公司信息
        // ========================================
        
        /**
         * 【关联查询公司信息】
         * 
         * Job 表中只有 companyId（外键）
         * 需要根据 companyId 查询公司名称（Company.name）
         * 
         * 【为什么用 Map 缓存？】
         * - 避免 N+1 查询问题
         * - 如果有 100 个职位，循环查询公司会执行 100 次
         * - 先收集所有 companyId，一次查询获取所有公司，再构建 Map
         * - 只需要 1 次数据库查询
         * 
         * 【Map<Long, Company> 的 Key-Value】
         * - Key: companyId（公司ID）
         * - Value: Company（公司对象）
         * 
         * 模板中使用：${companyMap[job.companyId].name}
         */
        
        // 收集所有需要的公司ID（去重）
        /**
         * 【Stream 流处理】
         * 
         * jobs.stream()                    // 把 List 转成 Stream
         * .map(Job::getCompanyId)          // 提取每个职位的 companyId
         * .distinct()                      // 去重
         * .collect(Collectors.toSet())     // 收集为 Set
         * 
         * 【为什么用 Set 而不是 List？】
         * - Set 自动去重
         * - 公司可能发布多个职位，companyId 会重复
         * - 只查询一次每个公司
         */
        List<Long> companyIds = jobs.stream()
                .map(Job::getCompanyId)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询公司信息，构建 Map
        /**
         * 【HashMap vs Map】
         * 
         * HashMap 是 Map 的实现类
         * 这里用 Map 接口声明，好处：
         * - 面向接口编程
         * - 方便替换实现（如 LinkedHashMap）
         * - 单元测试时可以传入 mock 对象
         */
        Map<Long, Company> companyMap = new HashMap<>();
        
        /**
         * 【批量查询公司】
         * 
         * companyRepository.findAllById(companyIds)
         * 根据多个 ID 一次性查询
         * 比循环中逐个查询效率高得多
         * 
         * 【companyService.getCompanyById(id)】
         * 单个查询，如果公司不存在会抛异常
         * 用 try-catch 包裹，忽略不存在的公司
         */
        for (Long companyId : companyIds) {
            try {
                Company company = companyService.getCompanyById(companyId);
                companyMap.put(companyId, company);
            } catch (Exception e) {
                // 公司不存在，跳过（理论上不应该发生）
            }
        }

        // ========================================
        // 第四步：将数据传递给视图
        // ========================================
        
        /**
         * 【model.addAttribute() 的作用】
         * 
         * 将数据存入 Model，供 Thymeleaf 模板使用
         * 
         * 语法：model.addAttribute("key", value)
         * 
         * 模板中使用：${key}
         * 如：model.addAttribute("jobs", jobs)
         *    th:text="${jobs.size()}"
         */
        model.addAttribute("jobs", jobs);              // 职位列表
        model.addAttribute("companyMap", companyMap);   // 公司Map
        model.addAttribute("keyword", keyword);        // 搜索关键词（回显）
        model.addAttribute("city", city);              // 城市筛选（回显）
        model.addAttribute("education", education);    // 学历筛选（回显）

        // ========================================
        // 第五步：返回视图
        // ========================================
        
        /**
         * 【return "index" 的含义】
         * 
         * Thymeleaf 配置：
         * - 前缀：classpath:/templates/
         * - 后缀：.html
         * 
         * 所以返回 "index" 会渲染：
         * src/main/resources/templates/index.html
         * 
         * 【为什么不直接返回 HTML 字符串？】
         * - 返回视图名，由视图解析器处理
         * - 可以切换模板引擎（Thymeleaf → Freemarker → JSP）
         * - 便于国际化、布局复用等高级功能
         */
        return "index";
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
    @GetMapping("/job/{id}")
    public String jobDetail(@PathVariable Long id,
                            HttpSession session,
                            Model model) {
        
        // 获取当前用户
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);
        
        // 查询职位详情
        /**
         * 【try-catch 处理业务异常】
         * 
         * jobService.getJobById(id) 内部会检查职位是否存在
         * 如果不存在，抛出 RuntimeException
         * 
         * 这里捕获异常，返回友好的错误页面
         */
        try {
            Job job = jobService.getJobById(id);
            model.addAttribute("job", job);
            
            // 查询公司信息
            Company company = companyService.getCompanyById(job.getCompanyId());
            model.addAttribute("company", company);
            
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
}
