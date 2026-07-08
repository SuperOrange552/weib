package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.dto.admin.LoginRequest;
import com.weib.dto.admin.LoginResponse;
import com.weib.entity.AdminRole;
import com.weib.entity.User;
import com.weib.repository.AdminRoleRepository;
import com.weib.repository.UserRepository;
import com.weib.service.admin.AdminAuthService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * 管理员认证控制器
 *
 * 处理管理员登录、身份验证、登出操作。
 * 登录接口无需认证，其余接口需要 JWT Token。
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    private final UserRepository userRepository;
    private final AdminRoleRepository adminRoleRepository;

    public AdminAuthController(AdminAuthService adminAuthService,
                               UserRepository userRepository,
                               AdminRoleRepository adminRoleRepository) {
        this.adminAuthService = adminAuthService;
        this.userRepository = userRepository;
        this.adminRoleRepository = adminRoleRepository;
    }

    /**
     * 管理员登录
     *
     * @param request 登录请求（username + password）
     * @return JWT Token + 管理员信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse resp = adminAuthService.login(request);
            return Result.success(resp);
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 获取当前登录管理员信息
     *
     * @return 管理员详情（从 SecurityContext 获取）
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Result.error(401, "未登录");
            }
            Long userId = Long.valueOf(auth.getPrincipal().toString());
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return Result.error(401, "用户不存在");
            AdminRole adminRole = adminRoleRepository.findByUserId(userId).orElse(null);

            java.util.Map<String, Object> info = new java.util.HashMap<>();
            info.put("id", user.getId());
            info.put("username", user.getUsername());
            info.put("nickname", user.getNickname());
            info.put("roleType", adminRole != null ? adminRole.getRoleType() : "viewer");
            return Result.success(info);
        } catch (Exception e) {
            return Result.error(401, "未登录");
        }
    }

    /**
     * 管理员登出
     *
     * 客户端应删除本地存储的 JWT Token。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
