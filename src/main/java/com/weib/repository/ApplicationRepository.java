package com.weib.repository;

import com.weib.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================
 * 【Repository 接口】投递记录数据访问层
 * ============================================
 * 
 * 提供职位投递相关的数据访问操作
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /**
     * 根据用户ID查询投递记录
     * 查询某个求职者的所有投递
     * 
     * @param userId 用户ID
     * @return 投递记录列表
     */
    List<Application> findByUserId(Long userId);

    /**
     * 根据职位ID查询投递记录
     * 查询某个职位收到的所有投递
     * 
     * @param jobId 职位ID
     * @return 投递记录列表
     */
    List<Application> findByJobId(Long jobId);

    /**
     * 根据职位ID和状态查询投递记录
     * 
     * @param jobId 职位ID
     * @param status 投递状态（pending/viewed/accepted/rejected等）
     * @return 符合条件的投递记录列表
     */
    List<Application> findByJobIdAndStatus(Long jobId, String status);

    /**
     * 根据职位ID和用户ID查询投递记录
     * 用于判断用户是否已投递该职位
     * 
     * @param jobId 职位ID
     * @param userId 用户ID
     * @return 投递记录（Optional包装）
     */
    Optional<Application> findByJobIdAndUserId(Long jobId, Long userId);

    /**
     * 根据状态查询投递记录
     */
    List<Application> findByStatus(String status);

    /**
     * 批量查询多个职位的投递记录（避免 N+1）
     */
    List<Application> findByJobIdIn(List<Long> jobIds);

    List<Application> findByJobIdInAndUserId(List<Long> jobIds, Long userId);

    /**
     * 统计指定用户的投递数量
     *
     * @param userId 用户ID
     * @return 投递数量
     */
    long countByUserId(Long userId);

    long countByJobId(Long jobId);
}
