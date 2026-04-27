package com.weib.repository;

import com.weib.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
