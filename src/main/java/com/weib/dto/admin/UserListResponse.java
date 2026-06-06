package com.weib.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户列表响应 DTO
 *
 * 用于管理后台用户管理列表的数据展示。
 * 包含用户基本信息 + 统计数量（简历数/投递数）。
 */
@Data
public class UserListResponse {
    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String role;
    private String status;
    private long resumeCount;
    private long applicationCount;
    private LocalDateTime createdAt;
}
