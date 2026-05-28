package com.weib.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 为所有页面的 Model 添加 CSRF token，确保所有表单和 AJAX 请求都能使用
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final String CSRF_TOKEN_ATTR = "csrf_token";
    private static final SecureRandom RANDOM = new SecureRandom();

    @ModelAttribute
    public void addCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        }
        String token = (String) session.getAttribute(CSRF_TOKEN_ATTR);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(CSRF_TOKEN_ATTR, token);
        }
        request.setAttribute(CSRF_TOKEN_ATTR, token);
    }
}
