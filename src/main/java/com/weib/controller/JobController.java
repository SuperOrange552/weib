package com.weib.controller;

import com.weib.entity.Application;
import com.weib.entity.Job;
import com.weib.entity.User;
import com.weib.service.ApplicationService;
import com.weib.service.JobService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PostMapping("/job/{id}/apply")
    @ResponseBody  // 返回 JSON 数据
    public Result<?> apply(@PathVariable Long id, HttpSession session) {
        
        // ========================================
        // 第一步：获取当前用户
        // ========================================
        
        /**
         * 【Session 获取用户】
         * 
         * 登录时：session.setAttribute("user", user)
         * 获取时：session.getAttribute("user")
         * 
         * 【为什么要转成 User 类型？】
         * Session 存的是 Object
         * 需要强转才能使用 User 的方法
         */
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
        
        // 加载职位和公司信息
        /**
         * 【关联查询】
         * 
         * Application 只有 jobId
         * 需要关联查询：
         * - Job：职位信息
         * - Company：公司信息
         * 
         * 【为什么不用 JOIN？】
         * - JPA 的 JOIN 查询比较复杂
         * - 这里用循环查询，代码更清晰
         * - 投递记录通常不多（几十条），性能没问题
         */
        for (Application app : applications) {
            try {
                Job job = jobService.getJobById(app.getJobId());
                app.setJobTitle(job.getTitle());
            } catch (Exception e) {
                app.setJobTitle("职位已删除");
            }
        }
        
        // 存入模型
        model.addAttribute("user", user);
        model.addAttribute("applications", applications);
        
        return "my-applications";
    }
}

/**
 * ============================================
 * 【统一返回结果类】Result<T>
 * ============================================
 * 
 * 为什么需要统一的返回格式？
 * 
 * 【之前的写法】
 * return "登录成功";           // 直接返回字符串
 * return Result.success(1);   // 返回统一格式
 * 
 * 【统一返回格式】
 * {
 *   "code": 200,           // 状态码
 *   "msg": "操作成功",       // 消息
 *   "data": {...}          // 数据
 * }
 * 
 * 【好处】
 * 1. 前端容易处理
 *    - code=200 表示成功
 *    - code=500 表示失败
 *    - 根据 code 判断显示什么
 * 
 * 2. 规范接口
 *    - 所有接口返回格式一致
 *    - 便于调试和联调
 * 
 * 3. 携带更多元信息
 *    - 成功/失败状态
 *    - 错误消息
 *    - 错误码（用于国际化等）
 * 
 * ----------------------------------------
 * 【泛型 T】Result<T>
 * ----------------------------------------
 * 
 * Result<Integer>  → 成功时返回整数
 * Result<String>   → 成功时返回字符串
 * Result<User>     → 成功时返回用户对象
 * Result<?>        → 成功时不返回数据
 * 
 * 【为什么不直接返回数据？】
 * - 需要区分成功和失败
 * - 需要携带错误信息
 * - 需要元数据（如总数、分页等）
 * 
 * ----------------------------------------
 * 【状态码设计】
 * ----------------------------------------
 * 
 * 200：成功
 * 400：请求参数错误
 * 401：未登录
 * 403：没有权限
 * 404：资源不存在
 * 500：服务器内部错误
 */

/**
 * ============================================
 * 【Result 类定义】
 * ============================================
 * 
 * 这是一个内部类，用于统一返回格式
 * 
 * 【为什么用 static 内部类？】
 * - 不需要访问外部类的实例
 * - 独立使用，不依赖外部类
 * - 类似于普通的顶层类
 * 
 * 【为什么不单独写一个文件？】
 * - 这是专门给 JobController 用的
 * - 只在这里使用
 * - 减少文件数量
 * 
 * 【实际项目中的做法】
 * - 通常放在单独的包：com.xxx.common
 * - 多个 Controller 共用
 * - 抽成公共模块
 */
class Result<T> {
    
    /**
     * 【状态码】
     * 
     * 200：成功
     * 其他：失败（前端根据 code 判断）
     */
    private int code;
    
    /**
     * 【消息】
     * 
     * 成功：返回成功提示
     * 失败：返回错误原因
     */
    private String msg;
    
    /**
     * 【数据】
     * 
     * 泛型，可以是任意类型
     * - Integer：ID、数量
     * - String：消息
     * - User：用户信息
     * - List：列表数据
     */
    private T data;
    
    // ========================================
    // 【构造方法】私有化，禁止外部 new
    // ========================================
    
    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
    
    // ========================================
    // 【静态工厂方法】
    // ========================================
    
    /**
     * 【成功结果】
     * 
     * code = 200
     * msg = "操作成功"
     * data = 传入的数据
     * 
     * 【使用场景】
     * return Result.success(user);      // 返回用户
     * return Result.success(list);      // 返回列表
     * return Result.success("删除成功"); // 返回消息
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    
    /**
     * 【成功结果（无数据）】
     * 
     * code = 200
     * msg = "操作成功"
     * data = null
     * 
     * 【使用场景】
     * return Result.success();  // 只提示成功，不返回数据
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }
    
    /**
     * 【成功结果（带消息）】
     * 
     * code = 200
     * msg = 自定义消息
     * data = null
     * 
     * 【使用场景】
     * return Result.success("投递成功！");
     */
    public static <T> Result<T> success(String msg) {
        return new Result<>(200, msg, null);
    }
    
    /**
     * 【错误结果】
     * 
     * code = 500
     * msg = 错误消息
     * data = null
     * 
     * 【使用场景】
     * return Result.error("用户名已存在");
     * return Result.error("服务器异常");
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
    
    /**
     * 【错误结果（带状态码）】
     * 
     * code = 自定义状态码
     * msg = 错误消息
     * data = null
     * 
     * 【使用场景】
     * return Result.error(401, "请先登录");
     */
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }
    
    // ========================================
    // 【Getter 方法】Lombok 省略
    // ========================================
    
    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public T getData() { return data; }
}

// ============================================
// 【Application 扩展】添加临时字段
// ============================================

/**
 * 【说明】
 * Application 实体类只有数据库字段
 * 模板中需要职位名称等关联信息
 * 
 * 【解决方案】
 * 方案1：在 Application 中添加 @Transient 字段
 *        - 优点：简单直接
 *        - 缺点：污染实体类
 * 
 * 方案2：创建新的 DTO 类
 *        - 优点：不影响原实体
 *        - 缺点：需要创建新类
 * 
 * 这里用方案1，在 Controller 中设置临时字段
 */
