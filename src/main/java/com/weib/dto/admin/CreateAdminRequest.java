package com.weib.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "创建子管理员请求。密码必须符合统一强度规则。")
public class CreateAdminRequest {
    @NotBlank @Size(min=3,max=32) @Pattern(regexp="^[a-zA-Z0-9_\u4e00-\u9fa5]+$")
    @Schema(description="用户名：3–32 位，仅允许字母、数字、下划线和中文", example="audit_user", minLength=3, maxLength=32, pattern="^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$")
    private String username;

    @NotBlank @Size(min=8,max=64)
    @Schema(description="密码：8–64 位，必须同时包含大写字母、小写字母和数字，且不能与用户名相同", example="TestAdmin123", minLength=8, maxLength=64, format="password")
    private String password;

    @NotBlank @Pattern(regexp="super_admin|auditor|viewer")
    @Schema(description="管理员角色", allowableValues={"super_admin","auditor","viewer"}, example="auditor")
    private String roleType;
}