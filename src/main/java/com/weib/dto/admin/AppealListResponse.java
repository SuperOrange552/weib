package com.weib.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AppealListResponse(Long id, Long sanctionId, Long userId, String reason,
                                 List<String> evidenceUrls, String status, String reviewReason,
                                 LocalDateTime reviewedAt, LocalDateTime createdAt) { }