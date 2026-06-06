package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.dto.admin.CompanyListResponse;
import com.weib.dto.admin.PageResponse;
import com.weib.service.admin.AdminCompanyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 公司审核控制器
 *
 * 提供公司审核列表、详情、审核通过/驳回操作。
 * 通过/驳回操作需要 super_admin 或 auditor 权限（由 SecurityFilterChain 控制）。
 */
@RestController
@RequestMapping("/api/admin/companies")
public class AdminCompanyController {
    private final AdminCompanyService service;

    public AdminCompanyController(AdminCompanyService service) {
        this.service = service;
    }

    /**
     * 公司审核列表（分页 + 筛选）
     *
     * @param page    页码（0-based）
     * @param size    每页大小
     * @param status  审核状态筛选
     * @param keyword 公司名搜索关键词
     * @return 分页公司列表
     */
    @GetMapping
    public Result<PageResponse<CompanyListResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Page<CompanyListResponse> result = service.listCompanies(status, keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return Result.success(PageResponse.of(result));
    }

    /**
     * 公司详情
     *
     * @param id 公司 ID
     * @return 公司详细信息
     */
    @GetMapping("/{id}")
    public Result<CompanyListResponse> detail(@PathVariable Long id) {
        return Result.success(service.getCompanyDetail(id));
    }

    /**
     * 审核通过公司
     *
     * @param id 公司 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        service.approve(getAdminId(), id);
        return Result.success();
    }

    /**
     * 驳回公司审核
     *
     * @param id   公司 ID
     * @param body 请求体（含驳回原因 reason）
     * @return 操作结果
     */
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.reject(getAdminId(), id, body.get("reason"));
        return Result.success();
    }

    /**
     * 从 SecurityContext 获取当前登录管理员 ID
     */
    private Long getAdminId() {
        return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
    }
}
