package com.weib.dto.admin;

import java.time.LocalDateTime;

public record AdminSearchResponse(String type, Long id, Long ownerId, String title,
                                  String subtitle, String avatar, String status,
                                  LocalDateTime createdAt) { }
