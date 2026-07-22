package com.weib.service;

import com.weib.cache.CacheInvalidationService;
import com.weib.cache.CacheKeys;
import com.weib.dto.forum.*;
import com.weib.entity.*;
import com.weib.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ForumService {
    private final ForumPostRepository postRepository;
    private final ForumSectionRepository sectionRepository;
    private final ForumPostFavoriteRepository favoriteRepository;
    private final ForumPostLikeRepository likeRepository;
    private final ForumCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final SanctionService sanctionService;
    private final com.weib.cache.CacheAsideService cache;
    private final CacheInvalidationService invalidation;
    private final IdentityService identityService;

    @Transactional(readOnly = true)
    public List<ForumSectionResponse> sections() {
        return sectionRepository.findByStatusOrderBySortOrderAsc("ACTIVE").stream().map(this::section).toList();
    }

    @Transactional(readOnly = true)
    public Page<ForumPostResponse> posts(Long sectionId, String q, Pageable pageable) {
        return postRepository.search(sectionId, "ACTIVE", q == null ? "" : q.trim(), pageable).map(this::post);
    }

    @Transactional(readOnly = true)
    public ForumPostResponse detail(Long id) {
        return postRepository.findByIdAndStatus(id, "ACTIVE").map(this::post)
                .orElseThrow(() -> new IllegalArgumentException("post not found"));
    }

    @Transactional
    public ForumPostResponse create(Long userId, ForumPostCreateRequest request) { return create(userId, "SEEKER", request); }

    @Transactional
    public ForumPostResponse create(Long userId, String authorRole, ForumPostCreateRequest request) {
        if (userId == null || !userRepository.existsById(userId)) throw new IllegalArgumentException("login required");
        sanctionService.assertAllowed(userId, "MUTE");
        sanctionService.assertAllowed(userId, "PUBLISH_BAN");
        if (request == null || request.sectionId() == null) throw new IllegalArgumentException("section required");
        sectionRepository.findById(request.sectionId()).filter(s -> "ACTIVE".equals(s.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("section not found"));
        String title = required(request.title(), 2, 120, "title");
        String content = required(request.content(), 2, 10000, "content");
        List<String> images = validateList(request.imageUrls(), 9, 500, "image");
        List<String> tags = normalizeTags(request.tags());
        ForumPost p = new ForumPost();
        p.setSectionId(request.sectionId()); p.setAuthorId(userId); p.setAuthorRole(identityService.requireEnabledRole(userId, authorRole)); p.setTitle(title); p.setContent(content);
        p.setImageUrls(String.join("|", images)); p.setTags(String.join(",", tags)); p.setStatus("ACTIVE");
        ForumPost saved = postRepository.save(p); invalidate(saved.getId()); return post(saved);
    }

    /** 软删除当前业务身份自己发布的帖子，供移动端自动化清理和用户主动删除使用。 */
    @Transactional
    public void deleteOwnPost(Long userId, String authorRole, Long postId) {
        ForumPost post = active(postId);
        String canonicalRole = identityService.requireEnabledRole(userId, authorRole);
        if (!Objects.equals(post.getAuthorId(), userId)
                || !canonicalRole.equalsIgnoreCase(post.getAuthorRole())) {
            throw new AccessDeniedException("无权删除他人或其他身份发布的帖子");
        }
        post.setStatus("DELETED");
        postRepository.save(post);
        invalidate(postId);
    }

    @Transactional
    public void like(Long userId, Long postId) {
        sanctionService.assertAllowed(userId, "MUTE"); ForumPost p = active(postId);
        if (!likeRepository.existsByPostIdAndUserId(postId, userId)) {
            ForumPostLike l = new ForumPostLike(); l.setPostId(postId); l.setUserId(userId); likeRepository.save(l);
            postRepository.incrementLikeCount(p.getId()); invalidate(postId);
        }
    }

    @Transactional
    public void unlike(Long userId, Long postId) {
        ForumPost p = active(postId);
        if (likeRepository.existsByPostIdAndUserId(postId, userId)) {
            likeRepository.deleteByPostIdAndUserId(postId, userId); postRepository.decrementLikeCount(p.getId()); invalidate(postId);
        }
    }

    @Transactional
    public void favorite(Long userId, Long postId) {
        sanctionService.assertAllowed(userId, "MUTE"); ForumPost p = active(postId);
        if (!favoriteRepository.existsByPostIdAndUserId(postId, userId)) {
            ForumPostFavorite f = new ForumPostFavorite(); f.setPostId(postId); f.setUserId(userId); favoriteRepository.save(f);
            postRepository.incrementFavoriteCount(p.getId()); invalidate(postId);
        }
    }

    @Transactional
    public void unfavorite(Long userId, Long postId) {
        ForumPost p = active(postId);
        if (favoriteRepository.existsByPostIdAndUserId(postId, userId)) {
            favoriteRepository.deleteByPostIdAndUserId(postId, userId); postRepository.decrementFavoriteCount(p.getId()); invalidate(postId);
        }
    }

    @Transactional
    public ForumCommentResponse comment(Long userId, Long postId, ForumCommentCreateRequest request) { return comment(userId, "SEEKER", postId, request); }

    @Transactional
    public ForumCommentResponse comment(Long userId, String authorRole, Long postId, ForumCommentCreateRequest request) {
        sanctionService.assertAllowed(userId, "MUTE"); active(postId);
        String content = required(request == null ? null : request.content(), 1, 2000, "comment");
        ForumComment c = new ForumComment(); c.setPostId(postId); c.setAuthorId(userId); c.setAuthorRole(identityService.requireEnabledRole(userId, authorRole)); c.setContent(content); c.setStatus("ACTIVE");
        ForumComment saved = commentRepository.save(c); postRepository.incrementCommentCount(postId); invalidate(postId); return commentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ForumCommentResponse> comments(Long postId) {
        active(postId); return commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, "ACTIVE").stream().map(this::commentResponse).toList();
    }

    private ForumPost active(Long id) { return postRepository.findByIdAndStatus(id, "ACTIVE").orElseThrow(() -> new IllegalArgumentException("post not found or hidden")); }
    private void invalidate(Long id) { invalidation.invalidate(CacheKeys.forumPost(id)); invalidation.invalidatePattern("cache:forum:posts:list:*"); }
    private ForumSectionResponse section(ForumSection s) { return new ForumSectionResponse(s.getId(), s.getName(), s.getSlug(), s.getDescription()); }
    private ForumPostResponse post(ForumPost p) { User u = userRepository.findById(p.getAuthorId()).orElse(null); RoleProfile profile = identityService.profile(p.getAuthorId(), p.getAuthorRole()).orElse(null); String name = profile != null && profile.getNickname() != null ? profile.getNickname() : displayName(u); String avatar = profile != null && profile.getAvatar() != null ? profile.getAvatar() : (u == null ? null : u.getAvatar()); return new ForumPostResponse(p.getId(), p.getSectionId(), p.getAuthorId(), p.getAuthorRole(), name, avatar, p.getTitle(), p.getContent(), split(p.getImageUrls(), "\\|"), split(p.getTags(), ","), p.getStatus(), nz(p.getLikeCount()), nz(p.getCommentCount()), nz(p.getFavoriteCount()), p.getCreatedAt(), p.getUpdatedAt()); }
    private ForumCommentResponse commentResponse(ForumComment c) { User u = userRepository.findById(c.getAuthorId()).orElse(null); RoleProfile profile = identityService.profile(c.getAuthorId(), c.getAuthorRole()).orElse(null); String name = profile != null && profile.getNickname() != null ? profile.getNickname() : displayName(u); String avatar = profile != null && profile.getAvatar() != null ? profile.getAvatar() : (u == null ? null : u.getAvatar()); return new ForumCommentResponse(c.getId(), c.getPostId(), c.getAuthorId(), c.getAuthorRole(), name, avatar, c.getContent(), c.getCreatedAt()); }
    private String displayName(User u) { return u == null ? "unknown" : (u.getNickname() == null || u.getNickname().isBlank() ? u.getUsername() : u.getNickname()); }
    private int nz(Integer n) { return n == null ? 0 : n; }
    private List<String> split(String s, String delimiter) { return s == null || s.isBlank() ? List.of() : Arrays.stream(s.split(delimiter, -1)).filter(v -> !v.isBlank()).toList(); }
    private String required(String s, int min, int max, String field) { if (s == null || s.trim().length() < min || s.trim().length() > max) throw new IllegalArgumentException(field + " length invalid"); return s.trim(); }
    private List<String> validateList(List<String> values, int max, int itemMax, String field) { if (values == null) return List.of(); if (values.size() > max) throw new IllegalArgumentException(field + " count exceeded"); return values.stream().map(v -> v == null ? "" : v.trim()).peek(v -> { if (v.isBlank() || v.length() > itemMax) throw new IllegalArgumentException(field + " invalid"); if (field.equals("image") && !(v.startsWith("/uploads/") || v.startsWith("http://") || v.startsWith("https://"))) throw new IllegalArgumentException("image url invalid"); }).toList(); }
    private List<String> normalizeTags(List<String> values) {
        if (values == null) return List.of();
        List<String> tags = values.stream()
                .filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split("[,，]", -1)))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (tags.size() > 5) throw new IllegalArgumentException("tag count exceeded");
        if (tags.stream().anyMatch(tag -> tag.length() > 30)) throw new IllegalArgumentException("tag invalid");
        return tags;
    }
}
