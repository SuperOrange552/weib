package com.weib.repository;

import com.weib.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ============================================
 * 【Repository 接口】职位数据访问层
 * ============================================
 * 
 * 提供职位相关的数据访问操作
 */
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    /**
     * 根据公司ID查询职位列表
     * 
     * @param companyId 公司ID
     * @return 该公司的职位列表
     */
    List<Job> findByCompanyId(Long companyId);

    /**
     * 根据状态查询职位列表
     * 
     * @param status 职位状态（active/inactive/closed等）
     * @return 指定状态的职位列表
     */
    List<Job> findByStatus(String status);

    /**
     * 根据公司ID和状态查询职位
     * 
     * @param companyId 公司ID
     * @param status 职位状态
     * @return 符合条件的职位列表
     */
    List<Job> findByCompanyIdAndStatus(Long companyId, String status);

    /**
     * 根据职位名称模糊查询（忽略大小写）
     * 
     * @param keyword 搜索关键词
     * @return 匹配的职位列表
     */
    List<Job> findByTitleContainingIgnoreCase(String keyword);

    /**
     * 根据城市查询职位
     * 
     * @param city 城市名称
     * @return 该城市的职位列表
     */
    List<Job> findByCity(String city);

    /**
     * 根据学历要求查询职位
     * 
     * @param education 学历要求
     * @return 符合学历要求的职位列表
     */
    List<Job> findByEducation(String education);

    /**
     * 根据状态查询职位，按创建时间降序排列
     * 
     * @param status 职位状态
     * @return 排序后的职位列表
     */
    List<Job> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 根据公司ID查询职位，按创建时间降序排列
     *
     * @param companyId 公司ID
     * @return 排序后的职位列表
     */
    List<Job> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    /**
     * 分页查询指定状态的职位，按创建时间降序排列
     */
    Page<Job> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    /**
     * 分页模糊搜索职位名称
     */
    Page<Job> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * 分页查询指定状态的职位
     */
    Page<Job> findByStatus(String status, Pageable pageable);
}
