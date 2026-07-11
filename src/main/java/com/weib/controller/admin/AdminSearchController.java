package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.dto.admin.AdminSearchDetailResponse;
import com.weib.dto.admin.AdminSearchResponse;
import com.weib.dto.admin.PageResponse;
import com.weib.service.admin.AdminSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/search")
public class AdminSearchController {
    private final AdminSearchService service;

    public AdminSearchController(AdminSearchService service) { this.service = service; }

    @GetMapping
    public Result<PageResponse<AdminSearchResponse>> search(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int bounded = Math.min(Math.max(size, 1), 50);
        return Result.success(PageResponse.of(service.search(type, q,
                PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt")), isSuperAdmin())));
    }

    @GetMapping("/{type}/{id}")
    public Result<AdminSearchDetailResponse> detail(@PathVariable String type, @PathVariable Long id) {
        return Result.success(service.detail(type, id, isSuperAdmin()));
    }

    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}