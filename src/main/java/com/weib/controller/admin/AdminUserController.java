package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.dto.admin.PageResponse;
import com.weib.dto.admin.UserDetailResponse;
import com.weib.dto.admin.UserListResponse;
import com.weib.service.admin.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器
 *
 * 提供用户列表、详情、封禁/解封操作、密码重置。
 * 所有操作仅 super_admin 可访问。
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {
    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    /**
     * 用户列表（分页 + 筛选）
     *
     * @param page    页码（0-based）
     * @param size    每页大小
     * @param role    角色筛选
     * @param status  状态筛选
     * @param keyword 用户名搜索关键词
     * @return 分页用户列表
     */
    @GetMapping
    public Result<PageResponse<UserListResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Page<UserListResponse> result = service.listUsers(role, status, keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return Result.success(PageResponse.of(result));
    }

    /**
     * 用户详情
     *
     * @param id 用户 ID
     * @return 用户详细信息（含简历）
     */
    @GetMapping("/{id}")
    public Result<UserDetailResponse> detail(@PathVariable Long id) {
        return Result.success(service.getUserDetail(id));
    }

    /**
     * 封禁用户
     *
     * @param id 用户 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/ban")
    public Result<Void> ban(@PathVariable Long id) {
        service.banUser(getAdminId(), id);
        return Result.success();
    }

    /**
     * 解封用户
     *
     * @param id 用户 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/unban")
    public Result<Void> unban(@PathVariable Long id) {
        service.unbanUser(getAdminId(), id);
        return Result.success();
    }

    /**
     * 重置用户密码（管理员操作）
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return Result.error("新密码不能为空");
        }
        service.resetPassword(getAdminId(), id, newPassword);
        return Result.success();
    }

    /**
     * 从 SecurityContext 获取当前登录管理员 ID
     */
    private Long getAdminId() {
        return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
    }
}
