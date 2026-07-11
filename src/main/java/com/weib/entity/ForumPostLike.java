package com.weib.entity;
import jakarta.persistence.*; import lombok.Data; import java.time.LocalDateTime;
@Entity @Table(name="forum_post_likes", uniqueConstraints=@UniqueConstraint(name="uk_forum_like_post_user",columnNames={"post_id","user_id"})) @Data
public class ForumPostLike { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="post_id",nullable=false) private Long postId; @Column(name="user_id",nullable=false) private Long userId; @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt; @PrePersist protected void onCreate(){createdAt=LocalDateTime.now();} }
