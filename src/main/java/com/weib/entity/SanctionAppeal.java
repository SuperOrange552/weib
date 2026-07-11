package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户针对处罚提交的申诉。 */
@Entity
@Table(name = "sanction_appeals", indexes = {
        @Index(name = "idx_appeals_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_appeals_status_created", columnList = "status,created_at"),
        @Index(name = "idx_appeals_sanction_status", columnList = "sanction_id,status")
})
@Data
public class SanctionAppeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sanction_id", nullable = false)
    private Long sanctionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evidence_urls", columnDefinition = "TEXT")
    private String evidenceUrls;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "review_reason", columnDefinition = "TEXT")
    private String reviewReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}