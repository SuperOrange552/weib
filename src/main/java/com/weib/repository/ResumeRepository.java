package com.weib.repository;

import com.weib.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================
 * 【Repository 接口】简历数据访问层
 * ============================================
 * 
 * 提供简历相关的数据访问操作
 */
@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    /**
     * 根据用户ID查询简历
     * 每个用户只有一份简历
     * 
     * @param userId 用户ID
     * @return 简历信息（Optional包装）
     */
    Optional<Resume> findByUserId(Long userId);

    /**
     * 检查用户是否已创建简历
     * 
     * @param userId 用户ID
     * @return true=已创建，false=未创建
     */
    boolean existsByUserId(Long userId);

    List<Resume> findByUserIdIn(List<Long> userIds);

    /**
     * 统计指定用户的简历数量
     *
     * @param userId 用户ID
     * @return 简历数量
     */
    long countByUserId(Long userId);

    @Query("select r from Resume r where lower(coalesce(r.realName, '')) like lower(concat('%', :keyword, '%')) " +
            "or lower(coalesce(r.school, '')) like lower(concat('%', :keyword, '%')) " +
            "or lower(coalesce(r.major, '')) like lower(concat('%', :keyword, '%'))")
    Page<Resume> search(@Param("keyword") String keyword, Pageable pageable);
}
