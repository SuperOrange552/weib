package com.weib.service;

import com.weib.entity.ForumPost;
import com.weib.entity.ForumSection;
import com.weib.repository.ForumPostFavoriteRepository;
import com.weib.repository.ForumPostLikeRepository;
import com.weib.repository.ForumPostRepository;
import com.weib.repository.ForumSectionRepository;
import com.weib.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
import org.mockito.ArgumentCaptor;
import com.weib.dto.forum.ForumPostCreateRequest;
import com.weib.entity.User;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ForumServiceTest {
    @Test
    void creatingPostNormalizesChineseAndEnglishCommaSeparatedTags() {
        ForumPostRepository posts = mock(ForumPostRepository.class);
        ForumSectionRepository sections = mock(ForumSectionRepository.class);
        UserRepository users = mock(UserRepository.class);
        IdentityService identities = mock(IdentityService.class);
        ForumSection section = new ForumSection();
        section.setId(1L); section.setStatus("ACTIVE");
        when(users.existsById(5L)).thenReturn(true);
        when(users.findById(5L)).thenReturn(Optional.of(new User()));
        when(sections.findById(1L)).thenReturn(Optional.of(section));
        when(identities.requireEnabledRole(5L, "SEEKER")).thenReturn("SEEKER");
        when(identities.profile(5L, "SEEKER")).thenReturn(Optional.empty());
        when(posts.save(any(ForumPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ForumService service = new ForumService(posts, sections,
                mock(ForumPostFavoriteRepository.class), mock(ForumPostLikeRepository.class),
                mock(com.weib.repository.ForumCommentRepository.class), users,
                mock(SanctionService.class), mock(com.weib.cache.CacheAsideService.class),
                mock(com.weib.cache.CacheInvalidationService.class), identities);

        service.create(5L, "SEEKER", new ForumPostCreateRequest(
                1L, "标签测试", "同时支持中英文逗号", List.of(), List.of("求职，面试", "经验,交流")));

        ArgumentCaptor<ForumPost> captor = ArgumentCaptor.forClass(ForumPost.class);
        verify(posts).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("求职,面试,经验,交流", captor.getValue().getTags());
    }

    @Test
    void likingPostIsIdempotentAndIncrementsCounterOnce() {
        ForumPostRepository posts = mock(ForumPostRepository.class);
        ForumPostLikeRepository likes = mock(ForumPostLikeRepository.class);
        when(posts.findByIdAndStatus(20L, "ACTIVE")).thenReturn(Optional.of(post(20L, 0)));
        when(likes.existsByPostIdAndUserId(20L, 5L)).thenReturn(false);

        ForumService service = new ForumService(posts, mock(ForumSectionRepository.class),
                mock(ForumPostFavoriteRepository.class), likes, mock(com.weib.repository.ForumCommentRepository.class),
                mock(UserRepository.class), mock(com.weib.service.SanctionService.class), mock(com.weib.cache.CacheAsideService.class),
                mock(com.weib.cache.CacheInvalidationService.class), mock(com.weib.service.IdentityService.class));
        service.like(5L, 20L);

        verify(likes).save(any());
        verify(posts).incrementLikeCount(20L);
    }

    private ForumPost post(Long id, int likes) {
        ForumPost post = new ForumPost(); post.setId(id); post.setLikeCount(likes); post.setStatus("ACTIVE"); return post;
    }
}
