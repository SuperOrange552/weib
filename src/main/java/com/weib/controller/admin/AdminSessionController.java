package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.session.*;
import com.weib.service.admin.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {
    private final SessionRegistryService registry;
    private final AuditLogService auditLogService;

    @GetMapping("/users/{userId}")
    public Result<Map<String, LoginSlot>> sessions(@PathVariable Long userId) {
        Map<String, LoginSlot> result = new LinkedHashMap<>();
        registry.current(userId, ClientType.WEB).ifPresent(v -> result.put("WEB", v));
        registry.current(userId, ClientType.MOBILE).ifPresent(v -> result.put("MOBILE", v));
        return Result.success(result);
    }

    @PostMapping("/users/{userId}/force-logout")
    @com.weib.security.Idempotent
    public Result<Void> forceLogout(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) return Result.error("操作原因不能为空");
        registry.invalidateAll(userId, SessionInvalidationReason.ADMIN_FORCED);
        auditLogService.log(adminId(), "force_logout", "user", userId, reason.trim());
        return Result.success();
    }

    private Long adminId() { return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()); }
}
