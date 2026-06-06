package com.weib.repository;

import com.weib.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================
 * 【Repository 接口】管理员角色数据访问层
 * ============================================
 *
 * 提供管理员角色的增删改查操作
 */
@Repository
public interface AdminRoleRepository extends JpaRepository<AdminRole, Long> {

    /**
     * 根据用户 ID 查询管理员角色
     *
     * @param userId 用户 ID
     * @return Optional 包装的管理员角色
     */
    Optional<AdminRole> findByUserId(Long userId);

    /**
     * 根据用户 ID 删除管理员角色
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(Long userId);
}
