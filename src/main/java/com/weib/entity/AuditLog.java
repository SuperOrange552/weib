package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ============================================
 * 【实体类】审核操作日志表映射
 * ============================================
 * 
 * 记录管理员的所有审核操作（通过/驳回/封禁/解封/批量下架等）
 * 用于操作追溯和安全审计
 */
@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 执行操作的管理员用户 ID
     */
    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    /**
     * 操作类型：approve_company / reject_company / approve_job / reject_job /
     *          ban_user / unban_user / batch_offline / create_admin / update_admin / disable_admin
     */
    @Column(nullable = false, length = 50)
    private String action;

    /**
     * 操作目标类型：company / job / user / admin
     */
    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;

    /**
     * 操作目标 ID（可为空，如批量操作可能不记录单个 ID）
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * 操作原因/备注（如驳回理由、封禁原因等）
     */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /**
     * 操作时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
