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

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // CSRF 保护：对所有状态变更端点进行 token 校验
        registry.addInterceptor(csrfInterceptor)
                .addPathPatterns(
                        "/login", "/register",
                        "/resume/save",
                        "/boss/**",
                        "/job/*/apply",
                        "/job/*/favorite",
                        "/chat/upload",
                        "/api/**"
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
                        "/static/**"
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
