package com.weib.repository;

import com.weib.entity.RoleProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleProfileRepository extends JpaRepository<RoleProfile, Long> {
    Optional<RoleProfile> findByUserIdAndRoleType(Long userId, String roleType);
}
