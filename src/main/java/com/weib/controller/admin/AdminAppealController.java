package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.dto.admin.AppealReviewRequest;
import com.weib.dto.admin.PageResponse;
import com.weib.dto.AppealResponse;
import com.weib.service.AppealService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/admin/appeals")
public class AdminAppealController {
    private final AppealService appealService;

    public AdminAppealController(AppealService appealService) {
        this.appealService = appealService;
    }

    @GetMapping
    public Result<PageResponse<AppealResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int bounded = Math.min(Math.max(size, 1), 100);
        return Result.success(PageResponse.of(appealService.list(status,
                PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @GetMapping("/{id}")
    public Result<AppealResponse> detail(@PathVariable Long id) {
        return Result.success(appealService.detail(id));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody AppealReviewRequest request) {
        appealService.approve(adminId(), id, request == null ? null : request.reason());
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody AppealReviewRequest request) {
        appealService.reject(adminId(), id, request == null ? null : request.reason());
        return Result.success();
    }

    private Long adminId() {
        return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
    }
}