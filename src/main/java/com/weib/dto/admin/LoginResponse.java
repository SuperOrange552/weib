package com.weib.dto.admin;

import lombok.Data;

/**
 * 管理员登录响应 DTO
 *
 * 包含 JWT token 及管理员基本信息（前端用于显示身份）。
 */
@Data
public class LoginResponse {
    private String token;
    private AdminInfo admin;

    /**
     * 管理员基本信息
     */
    @Data
    public static class AdminInfo {
        private Long id;
        private String username;
        private String nickname;
        private String roleType;
    }
}
