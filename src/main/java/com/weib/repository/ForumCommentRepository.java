package com.weib.repository;
import com.weib.entity.ForumComment; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ForumCommentRepository extends JpaRepository<ForumComment,Long>{List<ForumComment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId,String status);}
