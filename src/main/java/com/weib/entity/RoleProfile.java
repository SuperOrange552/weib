package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_profiles", uniqueConstraints =
        @UniqueConstraint(name = "uk_role_profile", columnNames = {"user_id", "role_type"}))
@Data
public class RoleProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_type", nullable = false, length = 20)
    private String roleType;

    @Column(length = 50)
    private String nickname;

    @Column(length = 500)
    private String avatar;

    @Column(length = 500)
    private String bio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        roleType = roleType == null ? null : roleType.toUpperCase(java.util.Locale.ROOT);
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}
