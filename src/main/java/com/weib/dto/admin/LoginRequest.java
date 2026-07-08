package com.weib.dto.admin;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 管理员登录请求 DTO
 */
@Data
public class LoginRequest {
    @NotBlank @Size(max=32)
    private String username;
    @NotBlank @Size(max=64)
    private String password;
}
