package com.weib.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record ComplaintListResponse(
        Long id,
        Long reporterId,
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
