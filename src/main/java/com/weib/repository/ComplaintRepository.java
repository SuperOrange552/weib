package com.weib.repository;

import com.weib.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
            Long reporterId, String targetType, Long targetId, String status);

    Page<Complaint> findByStatus(String status, Pageable pageable);

    Page<Complaint> findByTargetTypeAndStatus(String targetType, String status, Pageable pageable);

    List<Complaint> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
}
