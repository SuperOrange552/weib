package com.weib.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审核日志响应 DTO
 *
 * 用于管理后台操作日志列表的数据展示。
 * 包含操作人信息 + 操作目标 + 操作原因。
 */
@Data
public class AuditLogResponse {
    private Long id;
    private Long adminId;
    private String adminName;
    private String action;
    private String targetType;
    private Long targetId;
    private String reason;
    private LocalDateTime createdAt;
}
