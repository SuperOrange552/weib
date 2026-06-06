package com.weib.dto.admin;

import lombok.Data;

/**
 * 创建子管理员请求 DTO
 *
 * 超级管理员通过此接口创建新的子管理员账号。
 * roleType 可选值：super_admin / auditor / viewer
 */
@Data
public class CreateAdminRequest {
    private String username;
    private String password;
    private String roleType;
}
