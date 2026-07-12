package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_access_requests", uniqueConstraints =
        @UniqueConstraint(name = "uk_resume_access_pair", columnNames = {"seeker_id", "boss_id"}))
@Data
public class ResumeAccessRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "seeker_id", nullable = false) private Long seekerId;
    @Column(name = "boss_id", nullable = false) private Long bossId;
    @Column(name = "company_id", nullable = false) private Long companyId;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    @Column(name = "requested_at", nullable = false) private LocalDateTime requestedAt;
    @Column(name = "decided_at") private LocalDateTime decidedAt;
    @PrePersist void create() { if (requestedAt == null) requestedAt = LocalDateTime.now(); }
}
