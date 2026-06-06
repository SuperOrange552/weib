package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ============================================
 * 【实体类】简历表映射
 * ============================================
 */
@Entity
@Table(name = "resumes")
@Data
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(length = 50)
    private String realName;

    @Column(length = 10)
    private String gender;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String birthday;

    @Column(length = 20)
    private String education;

    @Column(length = 100)
    private String school;

    @Column(length = 100)
    private String major;

    @Column(columnDefinition = "TEXT")
    private String workExperience;

    @Column(columnDefinition = "TEXT")
    private String projectExperience;

    @Column(length = 1000)
    private String skills;

    @Column(length = 2000)
    private String selfIntroduction;

    @Column(length = 500)
    private String avatar;

    @Column(length = 500)
    private String attachmentPath;

    @Column(nullable = false, length = 20)
    private String status = "draft";

    @Column(nullable = false, updatable = false)
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