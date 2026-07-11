package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages", uniqueConstraints = @UniqueConstraint(name="uk_message_sender_client", columnNames={"senderId","senderRole","clientMessageId"}))
@Data
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String conversationId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 20)
    private String senderRole;

    @Column(nullable = false)
    private Long receiverId;

    @Column(nullable = false, length = 20)
    private String receiverRole;

    @Column(length = 64)
    private String clientMessageId;

    @Column(length = 2000)
    private String content;

    @Column(nullable = false, length = 20)
    private String messageType = "text";

    @Column(length = 500)
    private String fileName;

    @Column(length = 500)
    private String filePath;

    private Long fileSize;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
