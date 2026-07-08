package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.dto.admin.JobListResponse;
import com.weib.dto.admin.PageResponse;
import com.weib.service.admin.AdminJobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 职位审核控制器
 *
 * 提供职位审核列表、详情、审核通过/驳回、批量下架操作。
 * 通过/驳回/批量下架需要 super_admin 或 auditor 权限。
 */
@RestController
@RequestMapping("/api/admin/jobs")
public class AdminJobController {
    private final AdminJobService service;

    public AdminJobController(AdminJobService service) {
        this.service = service;
    }

    /**
     * 职位审核列表（分页 + 筛选）
     *
     * @param page    页码（0-based）
     * @param size    每页大小
     * @param status  审核状态筛选
     * @param keyword 职位标题搜索关键词
     * @return 分页职位列表（含公司名称）
     */
    @GetMapping
    public Result<PageResponse<JobListResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Page<JobListResponse> result = service.listJobs(status, keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return Result.success(PageResponse.of(result));
    }

    /**
     * 职位详情
     *
     * @param id 职位 ID
     * @return 职位详细信息
     */
    @GetMapping("/{id}")
    public Result<JobListResponse> detail(@PathVariable Long id) {
        return Result.success(service.getJobDetail(id));
    }

    /**
     * 审核通过职位
     *
     * @param id 职位 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/approve")
    @com.weib.security.Idempotent
    public Result<Void> approve(@PathVariable Long id) {
        service.approve(getAdminId(), id);
        return Result.success();
    }

    /**
     * 驳回职位审核
     *
     * @param id   职位 ID
     * @param body 请求体（含驳回原因 reason）
     * @return 操作结果
     */
    @PutMapping("/{id}/reject")
    @com.weib.security.Idempotent
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.reject(getAdminId(), id, body.get("reason"));
        return Result.success();
    }

    /**
     * 批量下架职位
     *
     * @param body 请求体（含 ids: [1, 2, 3]）
     * @return 成功下架数量
     */
    @PostMapping("/batch-offline")
    @com.weib.security.Idempotent
    public Result<Map<String, Object>> batchOffline(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要下架的职位");
        }
        int count = service.batchOffline(getAdminId(), ids);
        return Result.success(Map.of("successCount", count));
    }

    /**
     * 从 SecurityContext 获取当前登录管理员 ID
     */
    private Long getAdminId() {
        return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
    }
}
