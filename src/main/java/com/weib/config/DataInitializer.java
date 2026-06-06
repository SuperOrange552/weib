package com.weib.config;

import com.weib.entity.AdminRole;
import com.weib.entity.User;
import com.weib.repository.AdminRoleRepository;
import com.weib.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据初始化器
 *
 * 应用启动时自动检查并初始化必需数据：
 * - 创建超级管理员账号（admin / Admin@123456）
 * - 分配 super_admin 角色
 *
 * 所有操作幂等：已存在则跳过，可安全重复执行。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default-password:Admin@123456}")
    private String defaultPassword;

    public DataInitializer(UserRepository userRepository,
                           AdminRoleRepository adminRoleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminRoleRepository = adminRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initSuperAdmin();
    }

    private void initSuperAdmin() {
        // 检查是否已存在 admin 用户
        if (userRepository.findByUsername("admin").isPresent()) {
            log.info("超级管理员账号已存在，跳过初始化");
            return;
        }

        log.info("正在初始化超级管理员账号...");

        // 1. 创建 admin 用户
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(defaultPassword));
        admin.setNickname("超级管理员");
        admin.setRole("admin");
        admin.setStatus("active");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        admin = userRepository.save(admin);

        // 2. 创建 super_admin 角色
        AdminRole role = new AdminRole();
        role.setUserId(admin.getId());
        role.setRoleType("super_admin");
        role.setCreatedAt(LocalDateTime.now());
        adminRoleRepository.save(role);

        log.info("超级管理员初始化完成: admin / {} (角色: super_admin)", defaultPassword);
    }
}
