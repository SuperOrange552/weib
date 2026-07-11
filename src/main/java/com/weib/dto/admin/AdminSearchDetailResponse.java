package com.weib.dto.admin;

import java.util.Map;

public record AdminSearchDetailResponse(String type, Long id, Map<String, Object> data) { }
