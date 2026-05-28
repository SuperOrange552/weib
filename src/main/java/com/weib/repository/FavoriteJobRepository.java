package com.weib.repository;

import com.weib.entity.FavoriteJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteJobRepository extends JpaRepository<FavoriteJob, Long> {

    List<FavoriteJob> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<FavoriteJob> findByJobIdAndUserId(Long jobId, Long userId);

    boolean existsByJobIdAndUserId(Long jobId, Long userId);

    int countByUserId(Long userId);
}
