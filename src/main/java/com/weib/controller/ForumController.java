package com.weib.controller;

import com.weib.dto.Result;
import com.weib.dto.admin.PageResponse;
import com.weib.dto.forum.*;
import com.weib.entity.User;
import com.weib.service.ForumService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {
    private final ForumService service;
    @GetMapping("/sections") public Result<List<ForumSectionResponse>> sections(){return Result.success(service.sections());}
    @GetMapping("/posts") public Result<PageResponse<ForumPostResponse>> posts(@RequestParam(required=false) Long sectionId,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){int n=Math.min(Math.max(size,1),50);return Result.success(PageResponse.of(service.posts(sectionId,q,PageRequest.of(Math.max(page,0),n,Sort.by(Sort.Direction.DESC,"createdAt")))));}
    @GetMapping("/posts/{id}") public Result<ForumPostResponse> detail(@PathVariable Long id){return Result.success(service.detail(id));}
    @PostMapping("/posts") public Result<ForumPostResponse> create(@RequestBody ForumPostCreateRequest request,HttpSession session){return Result.success(service.create(userId(session),request));}
    @PostMapping("/posts/{id}/comments") public Result<ForumCommentResponse> comment(@PathVariable Long id,@RequestBody ForumCommentCreateRequest request,HttpSession session){return Result.success(service.comment(userId(session),id,request));}
    @GetMapping("/posts/{id}/comments") public Result<List<ForumCommentResponse>> comments(@PathVariable Long id){return Result.success(service.comments(id));}
    @PostMapping("/posts/{id}/like") public Result<Void> like(@PathVariable Long id,HttpSession session){service.like(userId(session),id);return Result.success();}
    @DeleteMapping("/posts/{id}/like") public Result<Void> unlike(@PathVariable Long id,HttpSession session){service.unlike(userId(session),id);return Result.success();}
    @PostMapping("/posts/{id}/favorite") public Result<Void> favorite(@PathVariable Long id,HttpSession session){service.favorite(userId(session),id);return Result.success();}
    @DeleteMapping("/posts/{id}/favorite") public Result<Void> unfavorite(@PathVariable Long id,HttpSession session){service.unfavorite(userId(session),id);return Result.success();}
    private Long userId(HttpSession session){User u=session==null?null:(User)session.getAttribute("user");if(u==null)throw new IllegalArgumentException("login required");return u.getId();}
}
