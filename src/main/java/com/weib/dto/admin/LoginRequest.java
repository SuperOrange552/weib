package com.weib.dto.admin;

import lombok.Data;

/**
 * 管理员登录请求 DTO
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
