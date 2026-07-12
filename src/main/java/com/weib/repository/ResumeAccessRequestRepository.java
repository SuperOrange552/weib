package com.weib.repository;

import com.weib.entity.ResumeAccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResumeAccessRequestRepository extends JpaRepository<ResumeAccessRequest, Long> {
    Optional<ResumeAccessRequest> findBySeekerIdAndBossId(Long seekerId, Long bossId);
    List<ResumeAccessRequest> findBySeekerIdOrderByRequestedAtDesc(Long seekerId);
    List<ResumeAccessRequest> findByBossIdOrderByRequestedAtDesc(Long bossId);
}
