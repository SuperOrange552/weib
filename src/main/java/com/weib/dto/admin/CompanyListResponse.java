package com.weib.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公司列表响应 DTO
 *
 * 用于管理后台公司审核列表的数据展示。
 * 包含公司基本信息 + 审核状态 + Boss 名称。
 */
@Data
public class CompanyListResponse {
    private Long id;
    private String name;
    private String industry;
    private String scale;
    private String address;
    private String description;
    private String bossName;
    private String auditStatus;
    private String auditReason;
    private LocalDateTime createdAt;
}
