package com.weib.service.admin;

import com.weib.dto.admin.DashboardStatsResponse;
import com.weib.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 仪表盘统计服务
 *
 * 预计算并缓存仪表盘核心指标：
 * - 每 5 分钟自动刷新缓存（@Scheduled）
 * - API 直接读取缓存，毫秒级响应
 * - 包含用户增长趋势（近 7 天）和职位行业分布
 */
@Service
@EnableScheduling
public class DashboardService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    private volatile DashboardStatsResponse cachedStats = new DashboardStatsResponse();

    /**
     * 启动时立即初始化缓存，避免首次请求返回空数据
     */
    @PostConstruct
    public void init() {
        refreshCache();
    }

    public DashboardService(UserRepository userRepository,
                            JobRepository jobRepository,
                            CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
    }

    /**
     * 定时刷新缓存（每 5 分钟）
     */
    @Scheduled(fixedRate = 300000)
    public void refreshCache() {
        cachedStats = computeStats();
    }

    /**
     * 获取仪表盘统计数据（从缓存读取）
     *
     * @return DashboardStatsResponse 缓存的统计数据
     */
    @Cacheable(value = "dashboard", key = "'stats'")
    public DashboardStatsResponse getStats() {
        return cachedStats;
    }

    /**
     * 计算完整统计数据
     *
     * 包含四项核心指标、近 7 天用户增长趋势、职位行业分布。
     *
     * @return 完整的仪表盘统计响应
     */
    @Transactional(readOnly = true)
    protected DashboardStatsResponse computeStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        // 核心指标
        stats.setTotalUsers(userRepository.count());
        stats.setTotalJobs(jobRepository.count());
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        stats.setTodayNewUsers(userRepository.countByCreatedAtAfter(todayStart));
        stats.setPendingCount(companyRepository.countByAuditStatus("pending")
                + jobRepository.countByAuditStatus("pending"));

        // 近 7 天用户增长（逐日统计）
        List<DashboardStatsResponse.UserGrowthPoint> growth = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            long count = userRepository.countByCreatedAtAfter(dayStart)
                    - userRepository.countByCreatedAtAfter(dayEnd);
            DashboardStatsResponse.UserGrowthPoint point = new DashboardStatsResponse.UserGrowthPoint();
            point.setDate(date.toString());
            point.setCount(Math.max(count, 0));
            growth.add(point);
        }
        stats.setUserGrowth(growth);

        // 职位行业分布（统计 Company.industry 的分布）
        List<Object[]> industryRows = companyRepository.countGroupByIndustry();
        List<DashboardStatsResponse.JobDistribution> dist = new ArrayList<>();
        for (Object[] row : industryRows) {
            DashboardStatsResponse.JobDistribution item = new DashboardStatsResponse.JobDistribution();
            item.setIndustry((String) row[0]);
            item.setCount((Long) row[1]);
            dist.add(item);
        }
        stats.setJobDistribution(dist);

        return stats;
    }
}
