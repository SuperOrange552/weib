package com.weib.config;

import com.weib.security.AdminJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 管理后台安全配置
 *
 * 双 SecurityFilterChain 架构：
 * - Chain #1（@Order 1）：匹配 /api/admin/**，JWT 无状态认证 + RBAC
 * - Chain #2（@Order 2）：透传所有其他路径，由现有拦截器（LoginInterceptor）接管
 *
 * 注意：passwordEncoder Bean 由 PasswordEncoderConfig 统一定义，此处不再重复声明
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class AdminSecurityConfig {

    private final AdminJwtAuthenticationFilter adminJwtFilter;

    public AdminSecurityConfig(AdminJwtAuthenticationFilter adminJwtFilter) {
        this.adminJwtFilter = adminJwtFilter;
    }

    /**
     * Chain #1 — 管理后台 API（高优先级）
     *
     * JWT 无状态认证，根据 adminRole 进行 RBAC 粒度控制：
     * - SUPER_ADMIN：全部权限
     * - AUDITOR：审核权限（companies、jobs、audit-logs、export、dashboard）
     * - VIEWER（默认）：仅 dashboard 只读
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/admin/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/api/admin/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/dashboard/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/companies/**").hasAnyRole("SUPER_ADMIN", "AUDITOR")
                .requestMatchers("/api/admin/jobs/**").hasAnyRole("SUPER_ADMIN", "AUDITOR")
                .requestMatchers("/api/admin/users/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/admin/complaints/**", "/api/admin/sanctions/**").hasAnyRole("SUPER_ADMIN", "AUDITOR")
                .requestMatchers("/api/admin/admins/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/admin/audit-logs/**").hasAnyRole("SUPER_ADMIN", "AUDITOR")
                .requestMatchers("/api/admin/export/**").hasAnyRole("SUPER_ADMIN", "AUDITOR")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, resp, authEx) -> {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write("{\"code\":401,\"msg\":\"未登录或Token已过期\",\"data\":null}");
                })
                .accessDeniedHandler((req, resp, accEx) -> {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter().write("{\"code\":403,\"msg\":\"权限不足\",\"data\":null}");
                })
            )
            .addFilterBefore(adminJwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Chain #2 — 透传链（低优先级）
     *
     * 所有非 /api/admin/** 的路径由现有拦截器（LoginInterceptor、CsrfInterceptor）处理。
     * 使用 @ConditionalOnMissingBean 防止与用户端安全配置冲突。
     */
    @Bean
    @Order(2)
    @ConditionalOnMissingBean(name = "userSecurityFilterChain")
    public SecurityFilterChain passThroughFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
