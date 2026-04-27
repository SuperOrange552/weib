package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ============================================
 * 【实体类】职位表映射
 * ============================================
 */
@Entity
@Table(name = "jobs")
@Data
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private Long companyId;

    private Integer salaryMin;

    private Integer salaryMax;

    @Column(length = 20)
    private String education;

    @Column(length = 20)
    private String experience;

    @Column(length = 50)
    private String city;

    @Column(length = 200)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(length = 500)
    private String tags;

    @Column(nullable = false, length = 20)
    private String status = "active";

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}