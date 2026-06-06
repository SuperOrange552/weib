package com.weib.controller.admin;

import com.weib.dto.Result;
import com.weib.dto.admin.DashboardStatsResponse;
import com.weib.entity.AuditLog;
import com.weib.service.admin.AuditLogService;
import com.weib.service.admin.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘控制器
 *
 * 提供管理后台仪表盘的核心指标、图表数据和最近审核记录。
 * stats 和 charts 接口对所有管理员角色开放；
 * recent-logs 仅 super_admin 和 auditor 可访问。
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final DashboardService dashboardService;
    private final AuditLogService auditLogService;

    public AdminDashboardController(DashboardService dashboardService, AuditLogService auditLogService) {
        this.dashboardService = dashboardService;
        this.auditLogService = auditLogService;
    }

    /**
     * 核心指标卡片
     *
     * @return 仪表盘统计（总用户数、总职位数、今日新增、待审核数）
     */
    @GetMapping("/stats")
    public Result<DashboardStatsResponse> stats() {
        return Result.success(dashboardService.getStats());
    }

    /**
     * 图表数据
     *
     * @return 用户增长趋势 + 职位行业分布
     */
    @GetMapping("/charts")
    public Result<?> charts() {
        DashboardStatsResponse stats = dashboardService.getStats();
        return Result.success(Map.of(
                "userGrowth", stats.getUserGrowth(),
                "jobDistribution", stats.getJobDistribution()
        ));
    }

    /**
     * 最近审核记录（仅 super_admin / auditor）
     *
     * @param limit 返回条数（默认 10）
     * @return 最近 N 条操作日志
     */
    @GetMapping("/recent-logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR')")
    public Result<List<AuditLog>> recentLogs(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(auditLogService.getRecentLogs(limit));
    }
}
