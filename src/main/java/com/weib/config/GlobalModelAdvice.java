package com.weib.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 为所有页面的 Model 添加 CSRF token，确保所有表单和 AJAX 请求都能使用
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final String CSRF_TOKEN_ATTR = "csrf_token";
    @ModelAttribute
    public void addCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            // 不强制创建 session，未登录用户访问公开页面不需要 session
            return;
        }
        String token = CsrfInterceptor.generateCsrfToken(session);
        request.setAttribute(CSRF_TOKEN_ATTR, token);
        Object activeRole = session.getAttribute("activeRole");
        if (activeRole != null) {
            request.setAttribute("activeRole", activeRole.toString());
        }
    }
}
