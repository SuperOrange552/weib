package com.weib.repository;
import com.weib.entity.ForumPostLike; import org.springframework.data.jpa.repository.JpaRepository;
public interface ForumPostLikeRepository extends JpaRepository<ForumPostLike,Long>{boolean existsByPostIdAndUserId(Long postId,Long userId);void deleteByPostIdAndUserId(Long postId,Long userId);}
