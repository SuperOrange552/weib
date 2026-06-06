package com.weib.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ============================================
 * 【配置类】密码加密配置
 * ============================================
 * 
 * 大白话：之前密码明文存库非常危险，
 * 现在用 BCrypt 加密后再存，数据库里看不到原始密码。
 * 
 * BCrypt 是什么？
 * - 一种慢哈希算法，专门为密码存储设计
 * - 每次加密结果不同（自带随机盐）
 * - 不可逆，即使数据库泄露也无法还原
 * 
 * 密码流程：
 * 注册：用户输入 "123456" → encode() → "$2a$10$..." 存库
 * 登录：用户输入 "123456" → matches() → 跟库里的 "$2a$10$..." 比对
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 注册 BCryptPasswordEncoder 为 Spring Bean
     * 其他类可以通过 @Autowired 或构造器注入使用
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 加密强度 10（默认值，耗时约 100ms，兼顾安全和性能）
        return new BCryptPasswordEncoder();
    }
}
