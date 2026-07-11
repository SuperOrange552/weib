package com.weib.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final CsrfInterceptor csrfInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final com.weib.security.IdempotencyInterceptor idempotencyInterceptor;

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 频率限制拦截器（最外层，优先级最高）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**");

        registry.addInterceptor(idempotencyInterceptor).addPathPatterns("/**");

        // CSRF 保护：对所有状态变更端点进行 token 校验
        registry.addInterceptor(csrfInterceptor)
                .addPathPatterns(
                        "/login", "/register",
                        "/resume/save",
                        "/boss/**",
                        "/job/*/apply",
                        "/job/*/favorite",
                        "/chat/**",
                        "/user/**",
                        "/api/**"
                )
                .excludePathPatterns(
                        "/api/admin/**",
                        "/api/seeker/**",
                        "/api/chat/**"
                );

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/captcha",
                        "/check-username",
                        "/company/**",
                        "/job/**",
                        "/",
                        "/index",
                        "/uploads/**",
                        "/chat/**",
                        "/ws/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/static/**",
                        "/api/admin/**",
                        "/admin/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
        // Chat files are served exclusively through ChatController.downloadFile()
        // which verifies user authentication and message ownership.
        // Direct file access via /chat/** is intentionally disabled.
    }
}
