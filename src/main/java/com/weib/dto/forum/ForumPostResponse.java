package com.weib.dto.forum;
import java.time.LocalDateTime; import java.util.List;
public record ForumPostResponse(Long id,Long sectionId,Long authorId,String authorRole,String authorName,String avatar,String title,String content,List<String> imageUrls,List<String> tags,String status,int likeCount,int commentCount,int favoriteCount,LocalDateTime createdAt,LocalDateTime updatedAt){}
