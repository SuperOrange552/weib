package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.entity.UserRole;
import com.weib.service.admin.AdminIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/identities")
@RequiredArgsConstructor
public class AdminIdentityController {
    private final AdminIdentityService service;

    @GetMapping("/users/{userId}")
    public Result<List<UserRole>> list(@PathVariable Long userId) { return Result.success(service.list(userId)); }

    @PutMapping("/users/{userId}/{role}/enable")
    @com.weib.security.Idempotent
    public Result<UserRole> enable(@PathVariable Long userId, @PathVariable String role,
                                   @RequestBody Map<String, String> body) {
        return Result.success(service.enable(adminId(), userId, role, body.get("reason")));
    }

    @PutMapping("/users/{userId}/{role}/disable")
    @com.weib.security.Idempotent
    public Result<UserRole> disable(@PathVariable Long userId, @PathVariable String role,
                                    @RequestBody Map<String, String> body) {
        return Result.success(service.disable(adminId(), userId, role, body.get("reason")));
    }

    private Long adminId() { return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()); }
}
