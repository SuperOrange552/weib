package com.weib.dto.admin;

import java.time.LocalDateTime;

public record SanctionResponse(
        Long id,
        Long userId,
        String sanctionType,
        String targetType,
        Long targetId,
        String reason,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String status,
        Long adminId) {
}
