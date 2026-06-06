package com.weib.security;

import com.weib.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 管理后台 JWT 认证过滤器
 *
 * 从 Authorization header 中提取 Bearer token，
 * 解析 JWT 中的 adminRole 和用户信息，设置到 SecurityContext。
 * 如果 token 缺失或无效，不设置认证信息（交由 SecurityFilterChain 返回 401）。
 */
@Component
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public AdminJwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.validateAdminToken(token);
            if (claims == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String adminRole = claims.get("adminRole", String.class);

            // 将数据库中的角色类型映射为 Spring Security 角色前缀
            String springRole = switch (adminRole) {
                case "super_admin" -> "ROLE_SUPER_ADMIN";
                case "auditor" -> "ROLE_AUDITOR";
                default -> "ROLE_VIEWER";
            };

            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority(springRole)
            );

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(username);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // Token 无效，清空上下文，继续过滤器链（由 SecurityFilterChain 返回 401）
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
