package com.weib.dto.admin;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 用户详情响应 DTO
 *
 * 继承 UserListResponse，追加邮箱和简历详情信息。
 * 用于管理员查看用户完整资料。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserDetailResponse extends UserListResponse {
    private String email;
    private List<ResumeInfo> resumeList;

    /**
     * 简历摘要信息（嵌套 DTO）
     */
    @Data
    public static class ResumeInfo {
        private Long id;
        private String realName;
        private String education;
        private String school;
        private String skills;
    }
}
