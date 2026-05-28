package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ============================================
 * 【实体类】公司表映射
 * ============================================
 */
@Entity
@Table(name = "companies")
@Data
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String logo;

    @Column(length = 50)
    private String industry;

    @Column(length = 20)
    private String scale;

    @Column(length = 2000)
    private String description;

    @Column(length = 200)
    private String address;

    // ========================================
    // 【地图坐标】用于高德地图打点
    // 纬度(latitude) 和 经度(longitude)
    // ========================================
    private Double latitude;   // 纬度，如 39.980557
    private Double longitude;  // 经度，如 116.337649

    @Column(length = 50)
    private String contactName;

    @Column(length = 20)
    private String contactPhone;

    @Column(length = 100)
    private String contactEmail;

    @Column(nullable = false, unique = true)
    private Long bossId;

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