package com.weib.controller.admin;

import com.weib.service.admin.CsvExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * CSV 导出控制器
 *
 * 提供用户列表和操作日志的 CSV 文件导出。
 * 仅 super_admin 和 auditor 可访问。
 */
@RestController
@RequestMapping("/api/admin/export")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR')")
public class AdminExportController {
    private final CsvExportService service;

    public AdminExportController(CsvExportService service) {
        this.service = service;
    }

    /**
     * 导出用户列表为 CSV
     *
     * @param role    角色筛选
     * @param status  状态筛选
     * @param keyword 关键词搜索
     * @return CSV 文件（UTF-8 with BOM）
     */
    @GetMapping("/users")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        String csv = service.exportUsers(role, status, keyword);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    /**
     * 导出操作日志为 CSV
     *
     * @param action    操作类型筛选
     * @param adminId   管理员 ID 筛选
     * @param startDate 起始时间
     * @param endDate   结束时间
     * @return CSV 文件（UTF-8 with BOM）
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        String csv = service.exportAuditLogs(action, adminId, startDate, endDate);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_logs.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
