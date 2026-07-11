package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 管理员对用户施加的可审计处罚。 */
@Entity
@Table(name = "user_sanctions", indexes = {
        @Index(name = "idx_sanctions_user_type_status", columnList = "user_id,sanction_type,status"),
        @Index(name = "idx_sanctions_window", columnList = "starts_at,ends_at"),
        @Index(name = "idx_sanctions_complaint", columnList = "source_complaint_id")
})
@Data
public class UserSanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sanction_type", nullable = false, length = 30)
    private String sanctionType;

    @Column(name = "target_type", length = 20)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "source_complaint_id")
    private Long sourceComplaintId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (startsAt == null) startsAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
