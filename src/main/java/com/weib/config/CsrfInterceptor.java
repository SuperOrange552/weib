package com.weib.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CsrfInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CsrfInterceptor.class);
    private static final String CSRF_TOKEN_ATTR = "csrf_token";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateCsrfToken(HttpSession session) {
        String token = (String) session.getAttribute(CSRF_TOKEN_ATTR);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(CSRF_TOKEN_ATTR, token);
        }
        return token;
    }

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
            // GET: 如果已有 session，确保 CSRF token 存在
            if (session != null) {
                String token = generateCsrfToken(session);
                request.setAttribute(CSRF_TOKEN_ATTR, token);
            }
            // session 为 null 时不强制创建，公开页面不需要 CSRF token
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

            log.warn("CSRF校验: URI={}, sessionToken存在={}, formToken长度={}, 匹配={}",
                    request.getRequestURI(),
                    sessionToken != null,
                    formToken != null ? formToken.length() : 0,
                    sessionToken != null && sessionToken.equals(formToken));

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
