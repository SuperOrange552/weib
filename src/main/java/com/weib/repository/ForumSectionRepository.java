package com.weib.repository;
import com.weib.entity.ForumSection; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ForumSectionRepository extends JpaRepository<ForumSection,Long>{List<ForumSection> findByStatusOrderBySortOrderAsc(String status);}
