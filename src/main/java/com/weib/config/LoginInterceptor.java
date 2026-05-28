package com.weib.config;

import com.weib.entity.User;
import com.weib.repository.UserRepository;
import com.weib.util.JwtUtil;
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

    private static final String CSRF_TOKEN_ATTR = "csrf_token";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        // 对 GET 请求确保 CSRF token 存在，供所有页面表单使用
        if ("GET".equalsIgnoreCase(request.getMethod())) {
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

        // 1. Session 有效，直接放行
        if (session != null && session.getAttribute("user") != null) {
            return true;
        }

        // 2. Session 无效，尝试 remember_token Cookie 自动登录
        User user = getUserFromRememberToken(request);
        if (user != null) {
            createSession(request, session, user);
            return true;
        }

        // 3. 尝试 JWT 认证（Cookie 或 Authorization Header）
        user = getUserFromJwt(request);
        if (user != null) {
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
                    return userRepository.findByRememberToken(token).orElse(null);
                }
            }
        }
        return null;
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
