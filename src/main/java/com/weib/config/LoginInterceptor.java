package com.weib.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * ============================================
 * 【拦截器】登录拦截器
 * ============================================
 * 
 * 什么是拦截器？
 * - 拦截器 = 在请求到达 Controller 之前/之后执行的组件
 * - 类似于过滤器（Filter），但更强大
 * - 可以在 Controller 执行前后做处理
 * 
 * 【拦截器 vs 过滤器】
 * 
 * 过滤器（Filter）：
 * - Servlet 规范，属于 Web 容器
 * - 在 DispatcherServlet 之前执行
 * - 只能拦截请求，不能获取 Spring 上下文
 * 
 * 拦截器（Interceptor）：
 * - Spring MVC 提供
 * - 在 DispatcherServlet 之后、Controller 之前执行
 * - 可以获取 Spring Bean
 * - 更强大，更灵活
 * 
 * 【执行顺序】
 * 请求 → Filter → DispatcherServlet → Interceptor → Controller
 * 
 * ----------------------------------------
 * 【HandlerInterceptor 接口】
 * ----------------------------------------
 * 
 * 三个方法：
 * 
 * 1. preHandle(request, response, handler)
 *    - 在 Controller 执行之前
 *    - 返回 true：继续执行
 *    - 返回 false：中断请求
 *    - 用途：登录检查、权限验证
 * 
 * 2. postHandle(request, response, handler, modelAndView)
 *    - 在 Controller 执行之后、视图渲染之前
 *    - 可以修改 ModelAndView
 *    - 用途：添加公共数据
 * 
 * 3. afterCompletion(request, response, handler, ex)
 *    - 在视图渲染之后
 *    - 可以获取异常
 *    - 用途：日志记录、性能统计
 * 
 * 【本拦截器的作用】
 * - 检查用户是否登录
 * - 未登录则跳转到登录页
 * - 保护需要登录才能访问的页面
 */
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 请求预处理
     * 
     * ----------------------------------------
     * 【方法功能】
     * ----------------------------------------
     * 
     * 在请求到达 Controller 之前执行：
     * 1. 检查 Session 中是否有用户信息
     * 2. 有：放行（return true）
     * 3. 没有：跳转登录页（return false）
     * 
     * ----------------------------------------
     * 【参数说明】
     * ----------------------------------------
     * 
     * HttpServletRequest request：
     * - HTTP 请求对象
     * - 可以获取请求路径、参数、Header 等
     * 
     * HttpServletResponse response：
     * - HTTP 响应对象
     * - 可以设置响应头、状态码、重定向等
     * 
     * Object handler：
     * - 目标处理器（Controller 方法）
     * - 可以获取方法信息
     * 
     * ----------------------------------------
     * 【为什么要这么写？】
     * ----------------------------------------
     * 
     * 1. session.getAttribute("user")
     *    - 登录成功时存入了 user
     *    - 如果能取到，说明已登录
     * 
     * 2. response.sendRedirect("/login")
     *    - 未登录，重定向到登录页
     *    - 浏览器地址栏会变成 /login
     * 
     * 3. return false
     *    - 中断请求，不再执行 Controller
     *    - 如果 return true，会继续执行
     * 
     * ----------------------------------------
     * 【执行流程】
     * ----------------------------------------
     * 
     * 用户访问 http://localhost:8080/
     * 
     * 1. DispatcherServlet 接收请求
     * 2. 执行拦截器 preHandle()
     * 3. 检查 Session
     *    - 有 user：return true → 继续执行 Controller
     *    - 无 user：重定向 + return false → 请求结束
     * 
     * 【配置拦截路径】
     * 在 WebConfig 中配置：
     * 
     * registry.addInterceptor(new LoginInterceptor())
     *     .addPathPatterns("/**")           // 拦截所有路径
     *     .excludePathPatterns("/login");   // 排除登录页
     * 
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  目标处理器
     * @return true=放行，false=中断
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        
        // 获取 Session（false 表示不存在不创建）
        HttpSession session = request.getSession(false);
        
        // 判断是否登录
        // 两个条件：
        // 1. session 不为 null（Session 存在）
        // 2. session 中有 user 属性（已登录）
        if (session == null || session.getAttribute("user") == null) {
            // 未登录：重定向到登录页
            response.sendRedirect("/login");
            return false;  // 中断请求
        }
        
        // 已登录：放行
        return true;
    }
}
