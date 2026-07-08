package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.dto.admin.CreateAdminRequest;
import com.weib.service.admin.AdminAdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

/**
 * 子管理员管理控制器
 *
 * 提供子管理员的列表、创建、角色修改、禁用操作。
 * 所有操作仅 super_admin 可访问。
 */
@RestController
@RequestMapping("/api/admin/admins")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminAdminController {
    private final AdminAdminService service;

    public AdminAdminController(AdminAdminService service) {
        this.service = service;
    }

    /**
     * 子管理员列表
     *
     * @return 所有子管理员信息（含角色类型）
     */
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.success(service.listAdmins());
    }

    /**
     * 创建子管理员
     *
     * @param request 创建请求（username + password + roleType）
     * @return 创建结果
     */
    @PostMapping
    @com.weib.security.Idempotent
    public Result<Map<String, Object>> create(@Valid @RequestBody CreateAdminRequest request) {
        return Result.success(service.createAdmin(request.getUsername(), request.getPassword(), request.getRoleType()));
    }

    /**
     * 修改子管理员角色
     *
     * @param userId 用户 ID
     * @param body   请求体（含 roleType）
     * @return 操作结果
     */
    @PutMapping("/{userId}")
    @com.weib.security.Idempotent
    public Result<Void> updateRole(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        if (isSelf(userId)) return Result.error("不能修改自己的角色");
        service.updateRole(userId, body.get("roleType"));
        return Result.success();
    }

    /**
     * 禁用子管理员
     *
     * @param userId 用户 ID
     * @return 操作结果
     */
    @PutMapping("/{userId}/disable")
    @com.weib.security.Idempotent
    public Result<Void> disable(@PathVariable Long userId) {
        if (isSelf(userId)) return Result.error("不能禁用自己");
        service.disable(userId);
        return Result.success();
    }

    private boolean isSelf(Long userId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return Long.valueOf(auth.getPrincipal().toString()).equals(userId);
    }
}
