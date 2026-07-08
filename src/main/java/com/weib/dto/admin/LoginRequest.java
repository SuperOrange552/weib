package com.weib.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "管理员登录请求")
public class LoginRequest {
    @NotBlank @Size(max=32)
    @Schema(description="用户名，最多 32 位", example="admin", maxLength=32)
    private String username;

    @NotBlank @Size(max=64)
    @Schema(description="密码输入最多 64 位", example="Admin123456", maxLength=64, format="password")
    private String password;
}