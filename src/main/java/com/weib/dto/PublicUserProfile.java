package com.weib.dto;

/** 不包含密码、Token 等敏感字段的公开用户资料。 */
public record PublicUserProfile(
        Long id,
        String role,
        String username,
        String nickname,
        String avatar,
        String status) {
}
