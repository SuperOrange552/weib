package com.weib.service.admin;

import com.weib.dto.admin.LoginRequest;
import com.weib.dto.admin.LoginResponse;
import com.weib.entity.AdminRole;
import com.weib.entity.User;
import com.weib.repository.AdminRoleRepository;
import com.weib.repository.UserRepository;
import com.weib.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 管理员认证服务
 *
 * 处理管理后台登录逻辑：
 * 1. 验证用户名密码
 * 2. 校验管理员身份（role = "admin"）
 * 3. 校验账号状态（status != "banned"）
 * 4. 查询管理员角色配置
 * 5. 生成 JWT Token 返回
 */
@Service
public class AdminAuthService {

    private final UserRepository userRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthService(UserRepository userRepository,
                            AdminRoleRepository adminRoleRepository,
                            JwtUtil jwtUtil,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminRoleRepository = adminRoleRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 管理后台登录
     *
     * @param request 登录请求（username + password）
     * @return 登录响应（JWT token + 管理员信息）
     * @throws RuntimeException 用户名或密码错误、非管理员账号、账号已禁用、角色未配置
     */
    @Transactional  // 需要写操作（更新 loginFailCount/lockUntil）
    public LoginResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户名或密码错误");
        }
        User user = userOpt.get();

        // 账户锁定检查
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("账号已被锁定，请15分钟后再试");
        }

        if (!"admin".equals(user.getRole())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if ("banned".equals(user.getStatus())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 登录失败：增加失败计数
            int failCount = (user.getLoginFailCount() != null ? user.getLoginFailCount() : 0) + 1;
            user.setLoginFailCount(failCount);
            if (failCount >= 5) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(15));
            }
            userRepository.save(user);
            throw new RuntimeException("用户名或密码错误");
        }

        // 登录成功：重置失败计数
        if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
            user.setLoginFailCount(0);
            user.setLockUntil(null);
            userRepository.save(user);
        }

        AdminRole adminRole = adminRoleRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("管理员角色未配置"));

        String token = jwtUtil.generateAdminToken(user.getId(), user.getUsername(), adminRole.getRoleType());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        LoginResponse.AdminInfo info = new LoginResponse.AdminInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setNickname(user.getNickname());
        info.setRoleType(adminRole.getRoleType());
        response.setAdmin(info);
        return response;
    }
}
