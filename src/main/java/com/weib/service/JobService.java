package com.weib.service;

import com.weib.entity.Job;
import com.weib.repository.JobRepository;
import com.weib.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ============================================
 * 【Service】职位业务逻辑层
 * ============================================
 * 
 * 职责：
 * - 职位列表查询
 * - 职位搜索
 * - 职位 CRUD 操作
 * - 职位统计
 * 
 * ----------------------------------------
 * 【@Service】Spring组件注解
 * ----------------------------------------
 * 
 * 作用：标记这是一个业务服务类
 * 
 * 特点：
 * - 自动被 Spring 扫描并注册为 Bean
 * - 注入到 Controller 中使用
 * 
 * ----------------------------------------
 * 【依赖注入】
 * ----------------------------------------
 * 
 * @RequiredArgsConstructor + private final
 * = 自动生成构造器，实现依赖注入
 * 
 * JobRepository：职位数据库操作
 * ApplicationRepository：投递记录数据库操作（用于检查是否已投递）
 */
@Service
@RequiredArgsConstructor
public class JobService {

    /**
     * 【依赖注入】职位Repository
     * 
     * Repository 负责数据库操作
     * Service 调用 Repository 完成数据持久化
     */
    private final JobRepository jobRepository;
    
    /**
     * 【依赖注入】投递记录Repository
     * 
     * 用于检查用户是否已投递职位
     */
    private final ApplicationRepository applicationRepository;

    /**
     * ========================================
     * 【获取所有活跃职位】
     * ========================================
     * 
     * 查询状态为 "active" 的职位
     * 按创建时间降序排列（最新发布在前）
     * 
     * 【@Transactional(readOnly = true)】
     * - 开启只读事务
     * - 查询操作不需要写锁
     * - 数据库可以进行优化
     * 
     * 【为什么要按时间排序？】
     * - 用户通常想看最新发布的职位
     * - 新职位更可能还在招聘
     * 
     * @return 活跃职位列表
     */
    @Transactional(readOnly = true)
    public List<Job> getAllActiveJobs() {
        return jobRepository.findByStatusOrderByCreatedAtDesc("active");
    }

    @Transactional(readOnly = true)
    public List<Job> getRecentActiveJobs(int limit) {
        return jobRepository.findByStatusOrderByCreatedAtDesc("active", PageRequest.of(0, limit)).getContent();
    }

    /**
     * ========================================
     * 【根据ID获取职位】
     * ========================================
     * 
     * @param id 职位ID
     * @return 职位对象
     * @throws RuntimeException 职位不存在时抛出
     * 
     * 【orElseThrow】
     * - 如果 Optional 为空（职位不存在）
     * - 抛出 RuntimeException
     * - 异常消息包含职位ID，方便排查
     */
    @Transactional(readOnly = true)
    // @Cacheable 不用于 JPA 实体 —— PageImpl 无默认构造器，反序列化会失败
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("职位不存在: " + id));
    }

    /**
     * ========================================
     * 【根据公司ID获取职位列表】
     * ========================================
     * 
     * 查询某公司发布的所有职位
     * 用于 Boss 管理自己的职位
     * 
     * @param companyId 公司ID
     * @return 该公司的职位列表
     */
    @Transactional(readOnly = true)
    public List<Job> getJobsByCompanyId(Long companyId) {
        return jobRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    /**
     * ========================================
     * 【搜索职位】
     * ========================================
     * 
     * 支持多条件组合搜索
     * - keyword：职位名称模糊匹配
     * - city：城市精确匹配
     * - education：学历要求精确匹配
     * 
     * 【为什么用 null 判断？】
     * - null 表示不筛选该条件
     * - 前端可以不传某些参数
     * - 后端根据是否为 null 决定是否过滤
     * 
     * 【为什么不直接 SQL 查询？】
     * - 动态 SQL 比较复杂
     * - Java 层面过滤代码更清晰
     * - 职位数量有限，性能没问题
     * 
     * @param keyword 搜索关键词（可选）
     * @param city 城市筛选（可选）
     * @param education 学历筛选（可选）
     * @return 符合条件的职位列表
     */
    @Transactional(readOnly = true)
    public List<Job> searchJobs(String keyword, String city, String education) {
        // 三个条件都为空，返回所有活跃职位
        if (keyword == null && city == null && education == null) {
            return jobRepository.findByStatusOrderByCreatedAtDesc("active");
        }

        // 第一步：用 keyword 过滤（无 keyword 则取所有活跃职位）
        /**
         * 【Repository 方法名查询】
         * 
         * findByTitleContainingIgnoreCase(keyword)
         * - 自动翻译成 SQL：
         *   SELECT * FROM jobs WHERE title LIKE '%keyword%'
         * - IgnoreCase = 忽略大小写
         */
        List<Job> result;
        if (keyword != null) {
            // 修复：keyword 搜索也只查活跃职位，避免返回已关闭职位
            result = jobRepository.findByTitleContainingIgnoreCaseAndStatus(keyword, "active");
        } else {
            result = jobRepository.findByStatus("active");
        }

        // 第二步：city 精确过滤
        if (city != null && !city.isEmpty()) {
            result = result.stream()
                    .filter(job -> city.equals(job.getCity()))
                    .toList();
        }

        // 第三步：education 精确过滤
        if (education != null && !education.isEmpty()) {
            result = result.stream()
                    .filter(job -> education.equals(job.getEducation()))
                    .toList();
        }

        return result;
    }

    /**
     * ========================================
     * 【创建职位】
     * ========================================
     * 
     * Boss 发布新职位
     * 
     * @Transactional
     * - 默认读写事务
     * - 需要 INSERT 操作
     * 
     * @param job 职位对象
     * @return 保存后的职位（包含生成的ID）
     */
    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public Job createJob(Job job) {
        return jobRepository.save(job);
    }

    /**
     * ========================================
     * 【更新职位】
     * ========================================
     * 
     * Boss 编辑已发布的职位
     * 
     * 【为什么要保留原创建时间？】
     * - createdAt 表示职位首次创建时间
     * - updateTime 表示最后更新时间
     * - 创建时间不应该因为编辑而改变
     * 
     * @param job 职位对象（包含更新的字段）
     * @return 保存后的职位
     */
    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public Job updateJob(Job job) {
        // 查询原职位，逐字段更新，避免传入对象字段缺失导致数据被覆盖为 null
        Job existing = getJobById(job.getId());
        if (job.getTitle() != null) existing.setTitle(job.getTitle());
        if (job.getSalaryMin() != null) existing.setSalaryMin(job.getSalaryMin());
        if (job.getSalaryMax() != null) existing.setSalaryMax(job.getSalaryMax());
        if (job.getEducation() != null) existing.setEducation(job.getEducation());
        if (job.getExperience() != null) existing.setExperience(job.getExperience());
        if (job.getCity() != null) existing.setCity(job.getCity());
        if (job.getAddress() != null) existing.setAddress(job.getAddress());
        if (job.getDescription() != null) existing.setDescription(job.getDescription());
        if (job.getRequirements() != null) existing.setRequirements(job.getRequirements());
        if (job.getTags() != null) existing.setTags(job.getTags());
        if (job.getStatus() != null) existing.setStatus(job.getStatus());
        // createdAt 和 companyId 不允许通过此方法修改
        return jobRepository.save(existing);
    }

    /**
     * ========================================
     * 【删除职位】
     * ========================================
     * 
     * 硬删除：从数据库中删除记录
     * 
     * 【建议使用软删除】
     * 实际项目中通常用 status = 'deleted'
     * 而不是真正删除数据
     * - 可以保留历史数据
     * - 方便数据统计
     * - 防止误删
     * 
     * @param id 职位ID
     */
    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }

    /**
     * ========================================
     * 【浏览量+1】
     * ========================================
     * 
     * 每次查看职位详情时调用
     * 统计职位的浏览次数
     * 
     * 【为什么不直接加？】
     * job.setViewCount(job.getViewCount() + 1);
     * 
     * 因为 JPA 的脏检查机制：
     * - 查询出来的对象被 Hibernate 管理
     * - 修改字段后，事务提交时自动更新
     * - 需要显式 save() 才能立即更新
     * 
     * @param id 职位ID
     */
    @Transactional
    public void incrementViewCount(Long id) {
        Job job = getJobById(id);
        job.setViewCount(job.getViewCount() + 1);
        jobRepository.save(job);
    }
    
    /**
     * ========================================
     * 【检查用户是否已投递职位】
     * ========================================
     * 
     * 用于判断：
     * - 职位详情页是否显示"已投递"按钮
     * - 防止用户重复投递
     * 
     * 【为什么在 JobService 而不是 ApplicationService？】
     * - 这个方法和职位相关
     * - 在职位详情页调用
     * - 方便聚合
     * 
     * @param jobId 职位ID
     * @param userId 用户ID
     * @return true=已投递，false=未投递
     */
    @Transactional(readOnly = true)
    public List<Job> getJobsByIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return jobRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public boolean hasApplied(Long jobId, Long userId) {
        return applicationRepository.findByJobIdAndUserId(jobId, userId).isPresent();
    }

    /**
     * 分页获取活跃职位
     */
    @Transactional(readOnly = true)
    // PageImpl 不可缓存（无默认构造器，反序列化失败）
    public Page<Job> getActiveJobsPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jobRepository.findByStatusOrderByCreatedAtDesc("active", pageable);
    }

    /**
     * 分页搜索职位
     */
    @Transactional(readOnly = true)
    public Page<Job> searchJobsPaged(String keyword, String city, String education,
                                      String experience,
                                      Integer salaryMin, Integer salaryMax,
                                      String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Job> allJobs;

        if (keyword != null && !keyword.isEmpty()) {
            // keyword 搜索：先查出所有匹配的活跃职位，再内存过滤+分页
            allJobs = jobRepository.findByTitleContainingIgnoreCaseAndStatus(keyword, "active");
        } else {
            // 无 keyword：查出所有活跃职位，内存过滤+分页（避免 JPA 分页后再手工分页导致的双重分页）
            allJobs = jobRepository.findByStatus("active");
        }

        List<Job> filtered = new ArrayList<>(allJobs);
        if (city != null && !city.isEmpty()) {
            filtered = filtered.stream().filter(j -> city.equals(j.getCity())).collect(java.util.stream.Collectors.toList());
        }
        if (education != null && !education.isEmpty()) {
            filtered = filtered.stream().filter(j -> education.equals(j.getEducation())).collect(java.util.stream.Collectors.toList());
        }
        if (experience != null && !experience.isEmpty()) {
            filtered = filtered.stream().filter(j -> experience.equals(j.getExperience())).collect(java.util.stream.Collectors.toList());
        }
        if (salaryMin != null) {
            filtered = filtered.stream().filter(j -> j.getSalaryMax() != null && j.getSalaryMax() >= salaryMin).collect(java.util.stream.Collectors.toList());
        }
        if (salaryMax != null) {
            filtered = filtered.stream().filter(j -> j.getSalaryMin() != null && j.getSalaryMin() <= salaryMax).collect(java.util.stream.Collectors.toList());
        }

        if ("salary_high".equals(sort)) {
            filtered.sort(Comparator.comparing(Job::getSalaryMax, Comparator.nullsLast(Comparator.reverseOrder())));
        } else if ("salary_low".equals(sort)) {
            filtered.sort(Comparator.comparing(Job::getSalaryMin, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        int total = filtered.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<Job> pageContent = start < total ? filtered.subList(start, end) : List.of();
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, total);
    }
}