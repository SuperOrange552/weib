package com.weib.config;

import com.weib.entity.User;
import com.weib.repository.UserRepository;
import com.weib.service.SanctionService;
import com.weib.util.JwtUtil;
import com.weib.security.ForumAccessPolicy;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final SanctionService sanctionService;

    private static final String CSRF_TOKEN_ATTR = "csrf_token";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        if (ForumAccessPolicy.isPublicRead(request.getMethod(), request.getRequestURI())) {
            return true;
        }

        // 对 GET 请求确保 CSRF token 存在，供所有页面表单使用
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            if (session != null) {
                String token = (String) session.getAttribute(CSRF_TOKEN_ATTR);
                if (token == null) {
                    byte[] bytes = new byte[32];
                    RANDOM.nextBytes(bytes);
                    token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    session.setAttribute(CSRF_TOKEN_ATTR, token);
                }
                request.setAttribute(CSRF_TOKEN_ATTR, token);
            }
            // session 为 null 时不强制创建，公开页面不需要 CSRF token
        }

        // 1. Session 有效，直接放行
        if (session != null && session.getAttribute("user") != null) {
            User sessionUser = (User) session.getAttribute("user");
            if (isBlocked(sessionUser) && !isAppealPath(request)) {
                session.invalidate();
                response.sendRedirect("/login?blocked");
                return false;
            }
            return true;
        }

        // 2. Session 无效，尝试 remember_token Cookie 自动登录
        User user = getUserFromRememberToken(request);
        if (user != null) {
            if (isBlocked(user) && !isAppealPath(request)) {
                response.sendRedirect("/login?blocked");
                return false;
            }
            createSession(request, session, user);
            return true;
        }

        // 3. 尝试 JWT 认证（Cookie 或 Authorization Header）
        user = getUserFromJwt(request);
        if (user != null) {
            if (isBlocked(user) && !isAppealPath(request)) {
                response.sendRedirect("/login?blocked");
                return false;
            }
            createSession(request, session, user);
            return true;
        }

        // 4. 未登录，重定向到登录页
        response.sendRedirect("/login");
        return false;
    }

    private void createSession(HttpServletRequest request, HttpSession oldSession, User user) {
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("user", user);
        newSession.setAttribute("username", user.getUsername());
        // 修复：自动登录后在新 Session 中立即生成 CSRF Token
        CsrfInterceptor.generateCsrfToken(newSession);
    }

    private boolean isAppealPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/appeal".equals(uri) || uri.startsWith("/api/appeals");
    }

    /** 检查用户是否已被封禁 */
    private boolean isBlocked(User user) {
        return "banned".equals(user.getStatus())
                || (user.getId() != null && sanctionService.hasActive(user.getId(), "ACCOUNT_BAN"));
    }

    private User getUserFromRememberToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("remember_token".equals(cookie.getName())) {
                String token = cookie.getValue();
                if (token != null && !token.isEmpty()) {
                    String hashedToken = hashToken(token);
                    return userRepository.findByRememberToken(hashedToken).orElse(null);
                }
            }
        }
        return null;
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private User getUserFromJwt(HttpServletRequest request) {
        String jwt = null;

        // 优先从 Authorization Header 获取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }

        // 其次从 Cookie 获取
        if (jwt == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt_token".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if (jwt == null || jwt.isEmpty()) {
            return null;
        }

        Claims claims = jwtUtil.validateToken(jwt);
        if (claims == null) {
            return null;
        }

        Long userId = jwtUtil.getUserId(claims);
        return userRepository.findById(userId).orElse(null);
    }
}
