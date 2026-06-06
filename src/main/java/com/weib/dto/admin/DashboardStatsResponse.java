package com.weib.dto.admin;

import lombok.Data;
import java.util.List;

/**
 * 仪表盘统计响应 DTO
 *
 * 包含平台核心指标、用户增长趋势和职位行业分布数据。
 * 由 DashboardService 预计算并缓存，每 5 分钟刷新。
 */
@Data
public class DashboardStatsResponse {
    /** 平台总用户数 */
    private long totalUsers;
    /** 平台总职位数 */
    private long totalJobs;
    /** 今日新增用户数 */
    private long todayNewUsers;
    /** 待审核总数（公司待审 + 职位待审） */
    private long pendingCount;
    /** 近 7 天日活用户增长趋势 */
    private List<UserGrowthPoint> userGrowth;
    /** 职位行业分布 */
    private List<JobDistribution> jobDistribution;

    /**
     * 单日用户增长数据点
     */
    @Data
    public static class UserGrowthPoint {
        private String date;
        private long count;
    }

    /**
     * 行业职位分布数据项
     */
    @Data
    public static class JobDistribution {
        private String industry;
        private long count;
    }
}
