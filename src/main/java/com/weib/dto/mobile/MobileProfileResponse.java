package com.weib.dto.mobile;

/** 移动端当前业务身份资料。 */
public record MobileProfileResponse(
        Long userId,
        String role,
        String nickname,
        String avatar
) {
}

