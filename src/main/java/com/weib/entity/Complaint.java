package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户对账号、职位、公司、简历或媒体资源的投诉。 */
@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaints_status_created", columnList = "status,created_at"),
        @Index(name = "idx_complaints_target", columnList = "target_type,target_id"),
        @Index(name = "idx_complaints_reporter_target", columnList = "reporter_id,target_type,target_id,status")
})
@Data
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

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
