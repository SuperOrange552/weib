package com.weib.repository;

import com.weib.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================
 * 【Repository 接口】审核日志数据访问层
 * ============================================
 *
 * 提供审核日志的增删改查和高级搜索功能
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 根据管理员 ID 分页查询操作日志，按时间降序
     *
     * @param adminId 管理员用户 ID
     * @param pageable 分页参数
     * @return 分页日志结果
     */
    Page<AuditLog> findByAdminIdOrderByCreatedAtDesc(Long adminId, Pageable pageable);

    /**
     * 根据操作类型分页查询日志，按时间降序
     *
     * @param action 操作类型
     * @param pageable 分页参数
     * @return 分页日志结果
     */
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    /**
     * 高级搜索：支持按操作类型、管理员 ID、时间范围组合查询
     * 所有参数均可选（为 null 时不作为过滤条件）
     *
     * @param action 操作类型（可选）
     * @param adminId 管理员 ID（可选）
     * @param startDate 起始时间（可选）
     * @param endDate 结束时间（可选）
     * @param pageable 分页参数
     * @return 分页日志结果
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:adminId IS NULL OR a.adminId = :adminId) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchLogs(@Param("action") String action,
                              @Param("adminId") Long adminId,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              Pageable pageable);

    /**
     * 查询最近 10 条操作日志（用于仪表盘）
     *
     * @return 最近 10 条日志
     */
    List<AuditLog> findTop10ByOrderByCreatedAtDesc();
}
