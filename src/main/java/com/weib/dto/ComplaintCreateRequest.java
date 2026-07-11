package com.weib.dto;

import java.util.List;

public record ComplaintCreateRequest(
        String targetType,
        Long targetId,
        String category,
        String description,
        List<String> evidenceUrls) {
}
