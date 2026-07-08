package com.weib.dto.admin;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 创建子管理员请求 DTO
 *
 * 超级管理员通过此接口创建新的子管理员账号。
 * roleType 可选值：super_admin / auditor / viewer
 */
@Data
public class CreateAdminRequest {
    @NotBlank @Size(min=3,max=32) @Pattern(regexp="^[a-zA-Z0-9_\u4e00-\u9fa5]+$")
    private String username;
    @NotBlank @Size(min=8,max=64)
    private String password;
    @NotBlank @Pattern(regexp="super_admin|auditor|viewer")
    private String roleType;
}
