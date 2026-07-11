package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.dto.admin.*;
import com.weib.service.admin.AdminComplaintService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 投诉审核和处罚管理 API。 */
@RestController
@RequestMapping("/api/admin")
public class AdminComplaintController {

    private final AdminComplaintService service;

    public AdminComplaintController(AdminComplaintService service) {
        this.service = service;
    }

    @GetMapping("/complaints")
    public Result<PageResponse<ComplaintListResponse>> complaints(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return Result.success(PageResponse.of(service.list(status,
                PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @GetMapping("/complaints/{id}")
    public Result<ComplaintListResponse> detail(@PathVariable Long id) {
        return Result.success(service.detail(id));
    }

    @PostMapping("/complaints/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.reject(adminId(), id, body == null ? null : body.get("reason"));
        return Result.success();
    }

    @PostMapping("/complaints/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id, @RequestBody ComplaintReviewRequest request) {
        if (request != null && request.sanction() != null && isPermanentAccountBan(request.sanction()) && !isSuperAdmin()) {
            return Result.error(403, "永久账号封禁需要超级管理员权限");
        }
        service.resolve(adminId(), id, request);
        return Result.success();
    }

    @PostMapping("/sanctions")
    public Result<SanctionResponse> createSanction(@RequestBody SanctionCreateRequest request) {
        if (request != null && isPermanentAccountBan(request) && !isSuperAdmin()) {
            return Result.error(403, "永久账号封禁需要超级管理员权限");
        }
        return Result.success(service.createSanction(adminId(), request));
    }

    @GetMapping("/sanctions")
    public Result<PageResponse<SanctionResponse>> sanctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return Result.success(PageResponse.of(service.listSanctions(
                PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @PostMapping("/sanctions/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id, @RequestBody Map<String, String> body) {
        if (!isSuperAdmin()) return Result.error(403, "撤销处罚需要超级管理员权限");
        service.revoke(adminId(), id, body == null ? null : body.get("reason"));
        return Result.success();
    }

    private Long adminId() {
        return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
    }

    private boolean isSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private boolean isPermanentAccountBan(SanctionCreateRequest request) {
        return request != null && "ACCOUNT_BAN".equalsIgnoreCase(request.sanctionType()) && request.endsAt() == null;
    }
}
