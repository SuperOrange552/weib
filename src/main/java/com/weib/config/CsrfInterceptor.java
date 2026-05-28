package com.weib.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CsrfInterceptor implements HandlerInterceptor {

    private static final String CSRF_TOKEN_ATTR = "csrf_token";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            // GET: 生成 CSRF token 放入 session 和 request
            if (session == null) {
                session = request.getSession(true);
            }
            String token = (String) session.getAttribute(CSRF_TOKEN_ATTR);
            if (token == null) {
                token = generateToken();
                session.setAttribute(CSRF_TOKEN_ATTR, token);
            }
            request.setAttribute(CSRF_TOKEN_ATTR, token);
            return true;
        }

        // POST / PUT / DELETE / PATCH: 校验 CSRF token
        if ("POST".equalsIgnoreCase(request.getMethod())
                || "PUT".equalsIgnoreCase(request.getMethod())
                || "DELETE".equalsIgnoreCase(request.getMethod())
                || "PATCH".equalsIgnoreCase(request.getMethod())) {
            if (session == null) {
                response.sendRedirect("/login");
                return false;
            }
            String sessionToken = (String) session.getAttribute(CSRF_TOKEN_ATTR);
            String formToken = request.getParameter("_csrf");
            if (formToken == null) {
                formToken = request.getHeader("X-CSRF-Token");
            }

            if (sessionToken == null || formToken == null || !sessionToken.equals(formToken)) {
                response.sendRedirect("/login");
                return false;
            }

            // 校验通过（不轮换 token，否则 AJAX 页面的 meta 标签中的旧 token 会导致后续请求失败）
            return true;
        }

        return true;
    }
}
