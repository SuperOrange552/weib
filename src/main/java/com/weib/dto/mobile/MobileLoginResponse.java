package com.weib.dto.mobile;

public record MobileLoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        MobileUser user
) {
    public record MobileUser(Long id, String username, String nickname, String avatar, String role) {
    }
}
