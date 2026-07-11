package com.weib.dto.admin;

import java.time.LocalDateTime;

public record SanctionCreateRequest(
        Long userId,
        String sanctionType,
        String targetType,
        Long targetId,
        Long sourceComplaintId,
        String reason,
        LocalDateTime startsAt,
        LocalDateTime endsAt) {
}
