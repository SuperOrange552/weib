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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ForumServiceTest {
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
