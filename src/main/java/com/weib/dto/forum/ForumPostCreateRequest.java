package com.weib.dto.forum;
import java.util.List;
public record ForumPostCreateRequest(Long sectionId,String title,String content,List<String> imageUrls,List<String> tags){}
