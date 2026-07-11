package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_roles", uniqueConstraints =
        @UniqueConstraint(name = "uk_user_role", columnNames = {"user_id", "role_type"}),
        indexes = @Index(name = "idx_user_roles_status", columnList = "role_type,status"))
@Data
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_type", nullable = false, length = 20)
    private String roleType;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    @Column(name = "enabled_by")
    private Long enabledBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (enabledAt == null && "ACTIVE".equalsIgnoreCase(status)) enabledAt = now;
        roleType = roleType == null ? null : roleType.toUpperCase(java.util.Locale.ROOT);
        status = status == null ? "ACTIVE" : status.toUpperCase(java.util.Locale.ROOT);
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
        roleType = roleType == null ? null : roleType.toUpperCase(java.util.Locale.ROOT);
        status = status == null ? "ACTIVE" : status.toUpperCase(java.util.Locale.ROOT);
    }
}
