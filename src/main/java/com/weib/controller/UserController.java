package com.weib.controller;

import com.weib.annotation.RateLimit;
import com.weib.entity.User;
import com.weib.service.UserService;
import com.weib.service.CaptchaService;
import com.weib.util.CookieUtil;
import com.weib.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================
 * 【Controller 类】控制器层
 * ============================================
 * 
 * 什么是 Controller？
 * - Controller = 控制器，负责接收 HTTP 请求并返回响应
 * - 是三层架构的最上层：Controller → Service → Repository
 * - 是用户和系统的入口
 * 
 * 【Controller 的职责】
 * 1. 接收请求（解析参数）
 * 2. 调用 Service 处理业务
 * 3. 返回响应（页面或数据）
 * 
 * 【Controller 不应该做什么】
 * - 不要写业务逻辑（放在 Service）
 * - 不要直接操作数据库（放在 Repository）
 * - 不要做复杂计算（放在 Service）
 * 
 * ----------------------------------------
 * 【@Controller】Spring MVC 控制器注解
 * ----------------------------------------
 * 
 * 作用：标记这个类是 Web 控制器
 * 
 * 特点：
 * - 是 @Component 的衍生注解
 * - 被 @ComponentScan 自动扫描并注册为 Bean
 * - 方法返回的是视图名称（HTML 页面）
 * 
 * 【@Controller vs @RestController】
 * 
 * @Controller：
 * - 返回视图页面（HTML）
 * - 方法返回 String，如 "login" → login.html
 * - 用于传统 Web 应用（服务端渲染）
 * 
 * @RestController：
 * - 返回数据（JSON）
 * - = @Controller + @ResponseBody
 * - 方法返回对象，自动转为 JSON
 * - 用于 RESTful API（前后端分离）
 * 
 * 【什么时候用哪个？】
 * - 返回 HTML 页面 → @Controller
 * - 返回 JSON 数据 → @RestController
 * - 本项目是传统 Web 应用，用 @Controller
 * 
 * ----------------------------------------
 * 【@RequiredArgsConstructor】Lombok 注解
 * ----------------------------------------
 * 
 * 作用：自动生成构造函数，用于依赖注入
 * 
 * 这里注入了 UserService：
 * - private final UserService userService;
 * - Spring 自动注入 UserService 实例
 * - 不需要写 @Autowired
 */
@Controller
@RequiredArgsConstructor
public class UserController {

    /**
     * UserService - 用户业务服务
     * 
     * 通过构造器注入，保证不为 null
     */
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;

    /**
     * 显示登录页面
     * 
     * ----------------------------------------
     * 【@GetMapping("/login")】请求映射注解
     * ----------------------------------------
     * 
     * 作用：将 HTTP GET 请求映射到这个方法
     * 
     * 属性：
     * - value/path：请求路径
     * - params：参数条件
     * - headers：请求头条件
     * 
     * 【@GetMapping 是组合注解】
     * = @RequestMapping(method = RequestMethod.GET)
     * 
     * 【类似的注解】
     * - @GetMapping   - GET 请求（查询）
     * - @PostMapping  - POST 请求（创建）
     * - @PutMapping   - PUT 请求（更新）
     * - @DeleteMapping- DELETE 请求（删除）
     * - @PatchMapping - PATCH 请求（部分更新）
     * 
     * 【RESTful 风格】
     * GET    /users     - 查询所有用户
     * GET    /users/1   - 查询 ID=1 的用户
     * POST   /users     - 创建用户
     * PUT    /users/1   - 更新 ID=1 的用户
     * DELETE /users/1   - 删除 ID=1 的用户
     * 
     * ----------------------------------------
     * 【方法功能】
     * ----------------------------------------
     * 
     * 用户访问 http://localhost:8080/login
     * 返回登录页面（login.html）
     * 
     * ----------------------------------------
     * 【为什么要这么写？】
     * ----------------------------------------
     * 
     * 1. GET 请求用于显示页面
     *    - 用户在浏览器输入地址，是 GET 请求
     *    - 直接返回页面
     * 
     * 2. 不需要任何参数
     *    - 只是显示页面，不需要数据
     * 
     * ----------------------------------------
     * 【执行流程】
     * ----------------------------------------
     * 
     * 1. 用户在浏览器输入 http://localhost:8080/login
     * 2. 浏览器发送 GET 请求
     * 3. DispatcherServlet（前端控制器）接收请求
     * 4. 根据路径 /login 找到这个方法
     * 5. 执行方法，返回 "login"
     * 6. 视图解析器找到 login.html
     * 7. 渲染页面返回给浏览器
     * 
     * 【返回值说明】
     * - 返回 "login"
     * - 视图解析器会找 templates/login.html
     * - 配置在 application.yml：
     *   spring.thymeleaf.prefix: classpath:/templates/
     *   spring.thymeleaf.suffix: .html
     * 
     * @return 视图名称 "login"
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";  // 返回 login.html 页面
    }

    /**
     * 显示注册页面
     * 
     * 同登录页面，只是路径和返回值不同
     */
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    /**
     * 处理登录请求
     * 
     * ----------------------------------------
     * 【@PostMapping("/login")】POST 请求映射
     * ----------------------------------------
     * 
     * 用户提交登录表单，是 POST 请求
     * 
     * 【为什么登录用 POST 不用 GET？】
     * - POST 请求体传参，不会显示在 URL 中
     * - 密码不应该出现在 URL 中（安全问题）
     * - GET 请求会被浏览器缓存
     * 
     * ----------------------------------------
     * 【@RequestParam】请求参数绑定
     * ----------------------------------------
     * 
     * 作用：将请求参数绑定到方法参数
     * 
     * 【表单参数】
     * <input name="username"> → @RequestParam String username
     * <input name="password"> → @RequestParam String password
     * 
     * 【属性】
     * - value/name：参数名（可省略）
     * - required：是否必填（默认 true）
     * - defaultValue：默认值
     * 
     * 【例子】
     * @RequestParam String username
     * - 参数名是 username，必填
     * 
     * @RequestParam("user") String username
     * - 参数名是 user，变量名是 username
     * 
     * @RequestParam(required = false) String nickname
     * - 可选参数，可能为 null
     * 
     * @RequestParam(defaultValue = "1") Integer page
     * - 默认值，不传就是 1
     * 
     * ----------------------------------------
     * 【HttpSession】会话对象
     * ----------------------------------------
     * 
     * 作用：存储用户会话信息
     * 
     * 【什么是 Session？】
     * - HTTP 是无状态协议，每次请求都是独立的
     * - Session 用于保存用户状态（如登录信息）
     * - 每个用户有独立的 Session
     * 
     * 【Session 原理】
     * 1. 用户第一次访问，服务器创建 Session
     * 2. 服务器返回 Session ID（JSESSIONID Cookie）
     * 3. 用户再次请求，带上 Cookie
     * 4. 服务器根据 ID 找到 Session
     * 
     * 【常用方法】
     * - setAttribute(name, value)：存储数据
     * - getAttribute(name)：获取数据
     * - removeAttribute(name)：删除数据
     * - invalidate()：销毁 Session（退出登录）
     * 
     * ----------------------------------------
     * 【Model】数据模型
     * ----------------------------------------
     * 
     * 作用：向视图传递数据
     * 
     * 【使用方式】
     * model.addAttribute("error", "用户名或密码错误");
     * 
     * 页面中通过 ${error} 获取：
     * <div th:text="${error}"></div>
     * 
     * ----------------------------------------
     * 【方法功能】
     * ----------------------------------------
     * 
     * 1. 接收用户名和密码
     * 2. 调用 Service 验证
     * 3. 成功：存入 Session，跳转首页
     * 4. 失败：返回错误信息，停留在登录页
     * 
     * ----------------------------------------
     * 【执行流程】
     * ----------------------------------------
     * 
     * 1. 用户提交表单
     *    POST /login
     *    username=admin&password=123456
     * 
     * 2. Spring MVC 解析参数
     *    username = "admin"
     *    password = "123456"
     * 
     * 3. 调用 Service
     *    userService.login("admin", "123456")
     * 
     * 4. 如果成功
     *    - 存入 Session：session.setAttribute("user", user)
     *    - 重定向：return "redirect:/"
     * 
     * 5. 如果失败
     *    - 存入错误信息：model.addAttribute("error", "...")
     *    - 返回登录页：return "login"
     * 
     * ----------------------------------------
     * 【redirect: vs 直接返回视图名】
     * ----------------------------------------
     * 
     * return "login";
     * - 转发到 login.html
     * - URL 不变（还是 /login）
     * - 共享 request
     * 
     * return "redirect:/";
     * - 重定向到 /
     * - URL 变成 /
     * - 新的 request
     * 
     * 【为什么登录成功用 redirect？】
     * - 避免表单重复提交
     * - 刷新页面不会再次提交登录
     * - URL 更清晰
     * 
     * @param username 用户名
     * @param password 密码
     * @param session  HTTP 会话
     * @param model    数据模型
     * @return 成功重定向首页，失败返回登录页
     */
    @RateLimit(maxRequests = 10, windowSeconds = 60, key = "ip")
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam String captcha,
                        HttpSession session,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Model model) {

        // 验证码校验
        if (!com.weib.security.CredentialPolicy.validLoginInput(username, password)) {
            model.addAttribute("error", "\u8d26\u53f7\u6216\u5bc6\u7801\u683c\u5f0f\u4e0d\u6b63\u786e");
            return "login";
        }

        if (captchaService.verify(session, captcha) != CaptchaService.VerifyStatus.VALID) {
            model.addAttribute("error", "验证码错误");
            return "login";
        }

        // 基本输入校验
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "用户名/手机号和密码不能为空");
            return "login";
        }

        // 调用 Service 验证登录（支持用户名或手机号）
        User user = userService.login(username, password).orElse(null);

        // 判断是否登录成功
        if (user == null) {
            // 检查是否因为账户被锁定
            if (userService.isAccountLocked(username)) {
                model.addAttribute("error", "账户已锁定，请15分钟后重试");
            } else {
                model.addAttribute("error", "用户名/手机号或密码错误");
            }
            return "login";
        }

        // 防止会话固定攻击：先销毁旧Session，再创建新Session
        session.invalidate();
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("user", user);
        newSession.setAttribute("username", user.getUsername());

        // 修复 BUG-012: 在新 Session 中立即生成 CSRF Token
        String csrfToken = com.weib.config.CsrfInterceptor.generateCsrfToken(newSession);
        newSession.setAttribute("csrf_token", csrfToken);

        // 生成记住我令牌，2 小时内自动登录
        String token = userService.generateRememberToken(user);
        CookieUtil.addRememberTokenCookie(response, token);

        // 生成 JWT 令牌，用于安全的身份认证传输
        String jwt = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        CookieUtil.addJwtCookie(response, jwt);

        // 重定向到首页
        return "redirect:/";
    }

    /**
     * 处理注册请求
     * 
     * ----------------------------------------
     * 【方法功能】
     * ----------------------------------------
     * 
     * 1. 接收注册信息
     * 2. 校验密码确认
     * 3. 校验长度
     * 4. 调用 Service 注册
     * 5. 成功跳转登录页，失败返回注册页
     * 
     * ----------------------------------------
     * 【为什么要这么写？】
     * ----------------------------------------
     * 
     * 1. 密码确认在前端做了，后端也要做
     *    - 前端验证是用户体验
     *    - 后端验证是安全保障
     * 
     * 2. 长度校验
     *    - 防止恶意输入超长数据
     *    - 数据库字段有长度限制
     * 
     * 3. 注册成功跳转登录页
     *    - 不自动登录（让用户手动登录）
     *    - 显示成功提示
     * 
     * @param username      用户名
     * @param password      密码
     * @param confirmPassword 确认密码
     * @param role 用户角色（seeker=求职者，boss=Boss）
     * @param model         数据模型
     * @return 成功跳转登录页，失败返回注册页
     */
    @RateLimit(maxRequests = 3, windowSeconds = 60, key = "ip")
    @PostMapping("/register")
    @com.weib.security.Idempotent
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam String phone,
                           @RequestParam String captcha,
                           @RequestParam(defaultValue = "seeker") String role,
                           Model model,
                           HttpSession session) {

        // 验证码校验
        if (captchaService.verify(session, captcha) != CaptchaService.VerifyStatus.VALID) {
            model.addAttribute("error", "验证码错误");
            return "register";
        }

        // 1. 密码确认校验
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "两次密码不一致");
            return "register";
        }

        // 2. 用户名格式校验
        if (username.length() < 3 || username.length() > 32) {
            model.addAttribute("error", "用户名长度3-32位");
            return "register";
        }
        if (!username.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$")) {
            model.addAttribute("error", "用户名只能包含字母、数字、下划线和中文");
            return "register";
        }

        // 3. 手机号校验
        if (phone == null || phone.isBlank()) {
            model.addAttribute("error", "手机号不能为空");
            return "register";
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            model.addAttribute("error", "手机号格式不正确");
            return "register";
        }

        // 4. 密码规则校验
        String pwdError = com.weib.security.CredentialPolicy.validatePassword(password, username, phone);
        if (pwdError != null) {
            model.addAttribute("error", pwdError);
            return "register";
        }

        // 5. 角色校验
        if (!"seeker".equals(role) && !"boss".equals(role)) {
            role = "seeker";
        }

        // 6. 调用 Service 注册
        User user = userService.register(username, password, phone, role);
        if (user == null) {
            model.addAttribute("error", "用户名或手机号已存在");
            return "register";
        }

        // 7. 成功：跳转登录页
        model.addAttribute("success", "注册成功，请登录");
        return "login";
    }

    /**
     * 退出登录
     * 
     * ----------------------------------------
     * 【方法功能】
     * ----------------------------------------
     * 
     * 1. 销毁 Session
     * 2. 跳转登录页
     * 
     * ----------------------------------------
     * 【session.invalidate()】
     * ----------------------------------------
     * 
     * 作用：销毁 Session
     * 
     * 效果：
     * - 删除服务端的 Session 对象
     * - 清除客户端的 JSESSIONID Cookie
     * - 用户状态清空
     * 
     * @param session HTTP 会话
     * @return 重定向到登录页
     */
    @PostMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response) {
        // 清除记住我令牌
        User user = (User) session.getAttribute("user");
        if (user != null) {
            userService.clearRememberToken(user);
        }
        session.invalidate();  // 销毁 Session
        CookieUtil.deleteRememberTokenCookie(response);
        CookieUtil.deleteJwtCookie(response);
        return "redirect:/login";
    }

    /**
     * 检查用户名是否存在（AJAX 接口）
     * 
     * ----------------------------------------
     * 【@ResponseBody】返回数据而非视图
     * ----------------------------------------
     * 
     * 作用：方法返回值直接作为响应体
     * 
     * 【为什么这里要用？】
     * - 这是 AJAX 接口，返回数据不是页面
     * - 返回 boolean → JSON 格式的 true/false
     * 
     * 【@GetMapping + @ResponseBody】
     * = @RequestMapping + @ResponseBody
     * 
     * 如果整个类都是 API，直接用 @RestController
     * 
     * ----------------------------------------
     * 【方法功能】
     * ----------------------------------------
     * 
     * 用户输入用户名时，前端 AJAX 调用此接口
     * 实时检查用户名是否已存在
     * 
     * ----------------------------------------
     * 【执行流程】
     * ----------------------------------------
     * 
     * 1. 用户在输入框输入用户名
     * 2. 前端 JavaScript 监听 input 事件
     * 3. 发送 AJAX 请求：
     *    GET /check-username?username=admin
     * 4. 后端返回 true 或 false
     * 5. 前端显示提示：
     *    - true：用户名已存在（红色）
     *    - false：用户名可用（绿色）
     * 
     * @param username 用户名
     * @return true=已存在，false=不存在
     */
    @RateLimit(maxRequests = 30, windowSeconds = 60, key = "ip")
    @GetMapping("/check-username")
    @ResponseBody
    public String checkUsername(@RequestParam String username,
                                @RequestParam(required = false) String phone,
                                HttpSession session) {
        // 限制未登录用户的查询频率
        Integer count = (Integer) session.getAttribute("check_username_count");
        if (count == null) count = 0;
        if (count >= 5) {
            return "rate_limited";
        }
        session.setAttribute("check_username_count", count + 1);

        if (userService.existsByUsername(username)) {
            return "taken";
        }
        if (phone != null && !phone.isBlank() && userService.existsByPhone(phone)) {
            return "phone_taken";
        }
        return "available";
    }

    /**
     * 修改密码页面
     */
    @GetMapping("/change-password")
    public String changePasswordPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "change-password";
    }

    /**
     * 提交修改密码
     */
    @PostMapping("/user/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                  @RequestParam String newPassword,
                                  @RequestParam String confirmPassword,
                                  HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "两次输入的新密码不一致");
            return "change-password";
        }

        try {
            userService.changePassword(user.getId(), oldPassword, newPassword);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "change-password";
        }

        // 修改成功后销毁会话，要求重新登录
        session.invalidate();
        return "redirect:/login?changed";
    }
}
