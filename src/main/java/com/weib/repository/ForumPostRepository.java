package com.weib.repository;
import com.weib.entity.ForumPost; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface ForumPostRepository extends JpaRepository<ForumPost,Long>{
 java.util.Optional<ForumPost> findByIdAndStatus(Long id,String status);
 @Query("select p from ForumPost p where p.status=:status and (:sectionId is null or p.sectionId=:sectionId) and (lower(p.title) like lower(concat('%',:q,'%')) or lower(p.content) like lower(concat('%',:q,'%')) or lower(coalesce(p.tags,'')) like lower(concat('%',:q,'%'))) order by p.createdAt desc") Page<ForumPost> search(@Param("sectionId") Long sectionId,@Param("status") String status,@Param("q") String q,Pageable pageable);
 @Modifying @Query("update ForumPost p set p.likeCount=p.likeCount+1 where p.id=:id") int incrementLikeCount(@Param("id") Long id);
 @Modifying @Query("update ForumPost p set p.likeCount=case when p.likeCount>0 then p.likeCount-1 else 0 end where p.id=:id") int decrementLikeCount(@Param("id") Long id);
 @Modifying @Query("update ForumPost p set p.favoriteCount=p.favoriteCount+1 where p.id=:id") int incrementFavoriteCount(@Param("id") Long id);
 @Modifying @Query("update ForumPost p set p.favoriteCount=case when p.favoriteCount>0 then p.favoriteCount-1 else 0 end where p.id=:id") int decrementFavoriteCount(@Param("id") Long id);
 @Modifying @Query("update ForumPost p set p.commentCount=p.commentCount+1 where p.id=:id") int incrementCommentCount(@Param("id") Long id);
}
