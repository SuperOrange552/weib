package com.weib.dto.forum;
import java.time.LocalDateTime;
public record ForumCommentResponse(Long id,Long postId,Long authorId,String authorRole,String authorName,String avatar,String content,LocalDateTime createdAt){}
