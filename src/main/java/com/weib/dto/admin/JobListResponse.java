package com.weib.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 职位列表响应 DTO
 *
 * 用于管理后台职位审核列表的数据展示。
 * 包含职位基本信息 + 所属公司名称 + 审核状态。
 */
@Data
public class JobListResponse {
    private Long id;
    private String title;
    private String companyName;
    private Long companyId;
    private Integer salaryMin;
    private Integer salaryMax;
    private String city;
    private String education;
    private String experience;
    private String description;
    private String auditStatus;
    private String auditReason;
    private LocalDateTime createdAt;
}
