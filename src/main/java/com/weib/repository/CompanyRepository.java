package com.weib.repository;

import com.weib.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================
 * 【Repository 接口】公司数据访问层
 * ============================================
 * 
 * 提供公司相关的数据访问操作
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    /**
     * 根据Boss ID查询公司列表
     * 
     * @param bossId Boss用户ID
     * @return 该Boss创建的公司列表
     */
    List<Company> findByBossId(Long bossId);

    /**
     * 根据Boss ID和公司ID查询公司
     * 用于验证公司是否属于该Boss
     * 
     * @param bossId Boss用户ID
     * @param id 公司ID
     * @return 公司信息（Optional包装）
     */
    Optional<Company> findByBossIdAndId(Long bossId, Long id);

    /**
     * 根据公司名称模糊查询（忽略大小写）
     * 
     * @param keyword 搜索关键词
     * @return 匹配的公司列表
     */
    List<Company> findByNameContainingIgnoreCase(String keyword);

    /**
     * 根据行业查询公司
     * 
     * @param industry 行业名称
     * @return 该行业的公司列表
     */
    List<Company> findByIndustry(String industry);

    /**
     * 检查Boss是否已创建公司
     * 
     * @param bossId Boss用户ID
     * @return true=已创建，false=未创建
     */
    boolean existsByBossId(Long bossId);

    // ========================================
    // 管理员审核相关查询
    // ========================================

    Page<Company> findByAuditStatusOrderByCreatedAtDesc(String auditStatus, Pageable pageable);
    Page<Company> findByNameContainingIgnoreCaseAndAuditStatus(String name, String auditStatus, Pageable pageable);
    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);
    long countByAuditStatus(String auditStatus);

    // ========================================
    // 仪表盘统计相关查询
    // ========================================

    /**
     * 统计各行业的公司数量（用于仪表盘行业分布图）
     *
     * @return List<Object[]> 每行 [industry: String, count: Long]，按 count 降序排列
     */
    @Query("SELECT c.industry, COUNT(c) FROM Company c WHERE c.industry IS NOT NULL AND c.industry != '' GROUP BY c.industry ORDER BY COUNT(c) DESC")
    List<Object[]> countGroupByIndustry();
}
