package com.weib.service;

import com.weib.entity.FavoriteJob;
import com.weib.repository.FavoriteJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteJobService {

    private final FavoriteJobRepository favoriteJobRepository;

    @Transactional
    public void toggleFavorite(Long jobId, Long userId) {
        favoriteJobRepository.findByJobIdAndUserId(jobId, userId)
                .ifPresentOrElse(
                        fav -> favoriteJobRepository.delete(fav),
                        () -> {
                            FavoriteJob fav = new FavoriteJob();
                            fav.setJobId(jobId);
                            fav.setUserId(userId);
                            favoriteJobRepository.save(fav);
                        }
                );
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Long jobId, Long userId) {
        return favoriteJobRepository.existsByJobIdAndUserId(jobId, userId);
    }

    @Transactional(readOnly = true)
    public List<FavoriteJob> getUserFavorites(Long userId) {
        return favoriteJobRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public int getFavoriteCount(Long userId) {
        return favoriteJobRepository.countByUserId(userId);
    }
}
