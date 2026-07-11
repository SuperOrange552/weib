package com.weib.repository;

import com.weib.entity.SanctionAppeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SanctionAppealRepository extends JpaRepository<SanctionAppeal, Long> {
    Optional<SanctionAppeal> findFirstBySanctionIdAndUserIdAndStatus(Long sanctionId, Long userId, String status);
    Page<SanctionAppeal> findByUserId(Long userId, Pageable pageable);
    Page<SanctionAppeal> findByStatus(String status, Pageable pageable);
}