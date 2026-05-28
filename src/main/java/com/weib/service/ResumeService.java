package com.weib.service;

import com.weib.entity.Resume;
import com.weib.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * ============================================
 * 【Service】简历业务逻辑层
 * ============================================
 * 
 * 职责：
 * - 简历的创建、查询、更新
 * - 简历状态管理
 * 
 * ----------------------------------------
 * 【@Service】Spring组件注解
 * ----------------------------------------
 * 
 * 作用：标记这是一个服务类
 * 
 * 特点：
 * - 是 @Component 的衍生注解
 * - 被 @ComponentScan 自动扫描
 * - 自动注册为 Spring Bean
 * 
 * 【@Service vs @Component】
 * - @Component：通用组件
 * - @Service：业务逻辑层专用
 * - @Repository：数据访问层专用
 * - @Controller：控制器层专用
 * 
 * 【为什么区分？】
 * - 代码语义更清晰
 * - 方便 AOP 切面拦截
 * - 方便单元测试时替换实现
 * 
 * ----------------------------------------
 * 【@Transactional】事务管理
 * ----------------------------------------
 * 
 * 作用：方法或类上的事务控制
 * 
 * 常见用法：
 * 1. 加在类上 → 所有方法都开启事务
 * 2. 加在方法上 → 只有这个方法开启事务
 * 3. readOnly = true → 读事务，性能更好
 * 
 * 【事务的 ACID 特性】
 * - Atomic（原子性）：操作要么全成功，要么全失败
 * - Consistency（一致性）：数据保持一致状态
 * - Isolation（隔离性）：并发操作互不影响
 * - Duration（持久性）：事务提交后数据永久保存
 * 
 * 【为什么 Service 层加事务？】
 * - Service 是业务逻辑层
 * - 可能有多个数据库操作
 * - 需要保证数据一致性
 * 
 * 【readOnly = true 的好处】
 * - 数据库只读不需要锁
 * - 性能更好
 * - 明确告诉数据库这是读操作
 */
@Service
@RequiredArgsConstructor
public class ResumeService {

    /**
     * 【依赖注入】Repository
     * 
     * private final + @RequiredArgsConstructor
     * = 自动生成构造器注入
     * 
     * 【ResumeRepository 的作用】
     * - 提供简历的 CRUD 操作
     * - 封装了 JPA 的基本方法
     * - 提供自定义查询方法
     */
    private final ResumeRepository resumeRepository;

    /**
     * ========================================
     * 【根据ID查询简历】
     * ========================================
     * 
     * @Transactional(readOnly = true)
     * - 开启只读事务
     * - 不需要写操作，性能更好
     * 
     * 【orElseThrow】
     * - 如果没找到简历，抛出异常
     * - 异常消息包含 userId，方便排查
     */
    @Transactional(readOnly = true)
    public Resume getResumeById(Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("简历不存在: id=" + id));
    }

    /**
     * ========================================
     * 【根据用户ID查询简历】
     * ========================================
     * 
     * 每个用户只有一份简历
     * 所以用 findByUserId 返回 Optional
     */
    @Transactional(readOnly = true)
    public Resume getResumeByUserId(Long userId) {
        return resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("简历不存在: userId=" + userId));
    }

    /**
     * ========================================
     * 【保存简历（新建或更新）】
     * ========================================
     * 
     * @Transactional（默认读写事务）
     * - save() 方法会执行 INSERT 或 UPDATE
     * - 需要写事务
     * 
     * 【save() 的工作原理】
     * - 如果 id 为 null → INSERT
     * - 如果 id 不为 null → UPDATE
     * 
     * 【为什么要自动设置状态？】
     * - 新简历默认为"草稿"
     * - 用户完善后可以"发布"
     * - 状态管理让用户更灵活
     */
    @Transactional
    public Resume saveResume(Resume resume) {
        if (resume.getId() == null) {
            // 防止同一用户创建多份简历
            if (resumeRepository.existsByUserId(resume.getUserId())) {
                Resume existing = getResumeByUserId(resume.getUserId());
                resume.setId(existing.getId());
                resume.setStatus(existing.getStatus());
            } else {
                resume.setStatus("draft");
            }
        }
        return resumeRepository.save(resume);
    }

    /**
     * ========================================
     * 【保存或更新简历】
     * ========================================
     * 
     * 这个方法和上面的是一样的
     * 保留是为了兼容不同的调用方式
     */
    @Transactional
    public Resume saveOrUpdateResume(Resume resume) {
        return resumeRepository.save(resume);
    }

    /**
     * ========================================
     * 【检查用户是否已创建简历】
     * ========================================
     * 
     * 用于判断：
     * - 能否投递职位（需要简历）
     * - 是否显示"完善简历"提示
     */
    @Transactional(readOnly = true)
    public boolean existsByUserId(Long userId) {
        return resumeRepository.existsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Resume> getResumeMapByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return resumeRepository.findByUserIdIn(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(Resume::getUserId, r -> r));
    }
}
