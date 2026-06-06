package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.dto.admin.AuditLogResponse;
import com.weib.dto.admin.PageResponse;
import com.weib.entity.AuditLog;
import com.weib.entity.User;
import com.weib.repository.UserRepository;
import com.weib.service.admin.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志控制器
 *
 * 提供审核操作日志的分页查询（支持多维筛选）。
 * 仅 super_admin 和 auditor 可访问。
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR')")
public class AdminAuditLogController {
    private final AuditLogService service;
    private final UserRepository userRepository;

    public AdminAuditLogController(AuditLogService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    /**
     * 操作日志列表（分页 + 多维筛选）
     *
     * @param page      页码（0-based）
     * @param size      每页大小
     * @param action    操作类型筛选（如 approve_company / ban_user）
     * @param adminId   操作人 ID 筛选
     * @param startDate 起始时间
     * @param endDate   结束时间
     * @return 分页日志列表（含操作人名称）
     */
    @GetMapping
    public Result<PageResponse<AuditLogResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Page<AuditLog> result = service.searchLogs(action, adminId, startDate, endDate,
                PageRequest.of(page, size));

        // 批量加载管理员名称，避免 N+1 查询
        List<Long> adminIds = result.getContent().stream()
                .map(AuditLog::getAdminId).distinct().toList();
        Map<Long, String> adminNameMap = userRepository.findAllById(adminIds).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getNickname() != null ? u.getNickname() : u.getUsername()));

        Page<AuditLogResponse> mapped = result.map(log -> {
            AuditLogResponse r = new AuditLogResponse();
            r.setId(log.getId());
            r.setAdminId(log.getAdminId());
            r.setAction(log.getAction());
            r.setTargetType(log.getTargetType());
            r.setTargetId(log.getTargetId());
            r.setReason(log.getReason());
            r.setCreatedAt(log.getCreatedAt());
            r.setAdminName(adminNameMap.getOrDefault(log.getAdminId(), "未知"));
            return r;
        });
        return Result.success(PageResponse.of(mapped));
    }
}
