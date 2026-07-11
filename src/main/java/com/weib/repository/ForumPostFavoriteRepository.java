package com.weib.repository;
import com.weib.entity.ForumPostFavorite; import org.springframework.data.jpa.repository.JpaRepository;
public interface ForumPostFavoriteRepository extends JpaRepository<ForumPostFavorite,Long>{boolean existsByPostIdAndUserId(Long postId,Long userId);void deleteByPostIdAndUserId(Long postId,Long userId);}
