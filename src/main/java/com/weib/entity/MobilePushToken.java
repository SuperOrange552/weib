package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name="mobile_push_tokens", uniqueConstraints=@UniqueConstraint(name="uk_push_installation",columnNames="installation_id"), indexes=@Index(name="idx_push_identity",columnList="user_id,active_role,status")) @Data
public class MobilePushToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="active_role",nullable=false,length=20) private String activeRole;
    @Column(name="installation_id",nullable=false,length=100) private String installationId;
    @Column(name="push_token",nullable=false,length=500) private String pushToken;
    @Column(nullable=false,length=20) private String status="ACTIVE";
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    @PrePersist @PreUpdate void touch(){updatedAt=LocalDateTime.now();}
}
