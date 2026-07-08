package com.weib.service.admin;

import com.weib.entity.AdminRole;
import com.weib.entity.User;
import com.weib.repository.AdminRoleRepository;
import com.weib.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 子管理员管理业务服务
 *
 * 提供子管理员的列表查询、创建、角色变更、禁用等管理操作。
 * 仅 super_admin 角色可调用此服务。
 */
@Service
public class AdminAdminService {
    private final UserRepository userRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAdminService(UserRepository userRepository, AdminRoleRepository adminRoleRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminRoleRepository = adminRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询所有子管理员列表
     *
     * 从 users 表中查询 role='admin' 的所有用户，
     * 并关联 admin_roles 表获取角色类型。
     *
     * @return 管理员信息列表（含 userId, username, nickname, status, roleType）
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAdmins() {
        List<User> admins = userRepository.findByRole("admin");
        List<Map<String, Object>> result = new ArrayList<>();
        for (User admin : admins) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", admin.getId());
            item.put("username", admin.getUsername());
            item.put("nickname", admin.getNickname());
            item.put("status", admin.getStatus());
            adminRoleRepository.findByUserId(admin.getId()).ifPresent(role -> {
                item.put("roleType", role.getRoleType());
                item.put("roleCreatedAt", role.getCreatedAt());
            });
            result.add(item);
        }
        return result;
    }

    /**
     * 创建子管理员
     *
     * 在 users 表中创建 role='admin' 的用户，
     * 同时在 admin_roles 表中创建对应的角色记录。
     * 密码使用 BCrypt 加密存储。
     *
     * @param username 管理员用户名
     * @param password 管理员密码（明文，将被加密存储）
     * @param roleType 角色类型（super_admin / auditor / viewer）
     * @return 创建结果（id, username, roleType）
     */
    @Transactional
    public Map<String, Object> createAdmin(String username, String password, String roleType) {
        String usernameError = com.weib.security.CredentialPolicy.validateUsername(username);
        if (usernameError != null) throw new IllegalArgumentException(usernameError);
        String passwordError = com.weib.security.CredentialPolicy.validatePassword(password, username, null);
        if (passwordError != null) throw new IllegalArgumentException(passwordError);
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(com.weib.security.CredentialPolicy.normalizeUsername(username));
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("admin");
        user.setNickname(username);
        user.setStatus("active");
        userRepository.save(user);

        AdminRole role = new AdminRole();
        role.setUserId(user.getId());
        role.setRoleType(roleType);
        adminRoleRepository.save(role);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("roleType", roleType);
        return result;
    }

    /**
     * 修改子管理员角色
     *
     * @param userId   用户 ID
     * @param roleType 新角色类型（super_admin / auditor / viewer）
     */
    @Transactional
    public void updateRole(Long userId, String roleType) {
        AdminRole role = adminRoleRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("管理员角色不存在: " + userId));
        role.setRoleType(roleType);
        adminRoleRepository.save(role);
    }

    /**
     * 禁用子管理员
     *
     * 将用户 status 设置为 banned，被禁管理员无法登录管理后台。
     * 注意：已在有效期内的 JWT Token 仍可使用，严格模式下应在 Filter 中实时校验。
     *
     * @param userId 用户 ID
     */
    @Transactional
    public void disable(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        user.setStatus("banned");
        userRepository.save(user);
    }
}
