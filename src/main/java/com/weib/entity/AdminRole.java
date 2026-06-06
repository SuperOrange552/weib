package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ============================================
 * 【实体类】管理员角色表映射
 * ============================================
 * 
 * 用于管理后台 RBAC 权限控制
 * 每个管理员用户在此表中有一条记录，记录其角色类型
 * 
 * 角色类型：
 * - super_admin：超级管理员（全权限）
 * - auditor：审核员（审核权限）
 * - viewer：观察者（只读权限）
 */
@Entity
@Table(name = "admin_roles")
@Data
public class AdminRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的用户 ID（与 users 表一对一关系）
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * 管理员角色类型：super_admin / auditor / viewer
     */
    @Column(name = "role_type", nullable = false, length = 20)
    private String roleType = "viewer";

    /**
     * 角色创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
