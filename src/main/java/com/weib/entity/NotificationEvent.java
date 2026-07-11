package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_events", uniqueConstraints = @UniqueConstraint(name = "uk_notification_event_id", columnNames = "event_id"),
        indexes = @Index(name = "idx_notification_recipient", columnList = "recipient_id,recipient_role,id"))
@Data
public class NotificationEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "event_id", nullable = false, length = 64) private String eventId;
    @Column(name = "recipient_id", nullable = false) private Long recipientId;
    @Column(name = "recipient_role", nullable = false, length = 20) private String recipientRole;
    @Column(name = "event_type", nullable = false, length = 50) private String eventType;
    @Column(name = "related_id") private Long relatedId;
    @Column(nullable = false, length = 500) private String title;
    @Column(columnDefinition = "TEXT") private String payload;
    @Column(name = "is_read", nullable = false) private Boolean isRead = false;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); }
}
