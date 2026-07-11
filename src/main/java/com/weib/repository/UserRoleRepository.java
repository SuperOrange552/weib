package com.weib.repository;

import com.weib.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserIdAndStatus(Long userId, String status);
    Optional<UserRole> findByUserIdAndRoleType(Long userId, String roleType);
    boolean existsByUserIdAndRoleTypeAndStatus(Long userId, String roleType, String status);
}
