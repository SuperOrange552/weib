package com.weib.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ============================================
 * 【实体类】投递记录表映射
 * ============================================
 * 
 * 投递记录 = 用户投递职位的行为记录
 * 
 * 【表关系】
 * - 一个用户可以投递多个职位
 * - 一个职位可以收到多个投递
 * - 用户和职位之间是多对多关系
 * - 投递记录就是这张关联表
 * 
 * 【字段说明】
 * - jobId：投递的职位ID（外键）
 * - userId：投递的用户ID（外键）
 * - resumeId：使用的简历ID（外键）
 * - status：投递状态
 * - bossNote：Boss的备注
 */
@Entity
@Table(name = "applications", uniqueConstraints = {
    @UniqueConstraint(name = "uk_job_user", columnNames = {"jobId", "userId"})
})
@Data
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long resumeId;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(length = 500)
    private String bossNote;
    
    /**
     * 【临时字段】职位名称（不存入数据库）
     * 
     * 用于在投递列表中显示职位名称
     * 
     * @Transient 注解表示这不是数据库字段
     * 只存在于内存中，不参与数据库操作
     */
    @Transient
    private String jobTitle;

    /** 面试时间 */
    private LocalDateTime interviewTime;

    /** 面试地点（空=线上面试） */
    @Column(length = 200)
    private String interviewLocation;

    /** 拒绝原因 */
    @Column(length = 1000)
    private String rejectReason;
    
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