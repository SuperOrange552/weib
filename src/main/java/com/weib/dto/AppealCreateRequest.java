package com.weib.dto;

import java.util.List;

public record AppealCreateRequest(Long sanctionId, String reason, List<String> evidenceUrls) { }