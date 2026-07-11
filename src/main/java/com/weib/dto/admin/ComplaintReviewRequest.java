package com.weib.dto.admin;

public record ComplaintReviewRequest(
        String reason,
        String contentAction,
        SanctionCreateRequest sanction) {
}
