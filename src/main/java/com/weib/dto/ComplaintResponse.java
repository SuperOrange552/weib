package com.weib.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ComplaintResponse(
        Long id,
        String targetType,
        Long targetId,
        String category,
        String description,
        List<String> evidenceUrls,
        String status,
        String reviewReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt) {
}
