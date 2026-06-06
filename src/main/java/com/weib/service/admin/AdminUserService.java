package com.weib.service.admin;

import com.weib.dto.admin.UserDetailResponse;
import com.weib.dto.admin.UserListResponse;
import com.weib.entity.Resume;
import com.weib.entity.User;
import com.weib.repository.ApplicationRepository;
import com.weib.repository.ResumeRepository;
import com.weib.repository.UserRepository;
import com.weib.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户管理业务服务
 *
 * 提供用户列表查询、用户详情查看、用户封禁/解封等管理操作。
 * 封禁/解封操作记录到 audit_log 用于操作追溯。
 */
@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationRepository applicationRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public AdminUserService(UserRepository userRepository, ResumeRepository resumeRepository,
                            ApplicationRepository applicationRepository, AuditLogService auditLogService,
                            UserService userService) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.applicationRepository = applicationRepository;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    /**
     * 分页查询用户列表（支持角色/状态筛选 + 关键词搜索）
     *
     * 参数组合逻辑：根据非空参数动态选择 Repository 方法。
     * 所有参数均可选，无参数时返回全部用户。
     *
     * @param role     角色筛选（seeker/boss/admin）
     * @param status   状态筛选（active/banned）
     * @param keyword  搜索关键词（模糊匹配用户名）
     * @param pageable 分页参数
     * @return 分页用户列表
     */
    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsers(String role, String status, String keyword, Pageable pageable) {
        Page<User> page;
        boolean hasRole = role != null && !role.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (hasKeyword && hasRole && hasStatus) {
            page = userRepository.findByUsernameContainingIgnoreCaseAndRoleAndStatus(keyword, role, status, pageable);
        } else if (hasKeyword && hasRole) {
            page = userRepository.findByUsernameContainingIgnoreCaseAndRole(keyword, role, pageable);
        } else if (hasKeyword && hasStatus) {
            page = userRepository.findByUsernameContainingIgnoreCaseAndStatus(keyword, status, pageable);
        } else if (hasKeyword) {
            page = userRepository.findByUsernameContainingIgnoreCase(keyword, pageable);
        } else if (hasRole && hasStatus) {
            page = userRepository.findByRoleAndStatus(role, status, pageable);
        } else if (hasRole) {
            page = userRepository.findByRole(role, pageable);
        } else if (hasStatus) {
            page = userRepository.findByStatus(status, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }

        return page.map(this::toListResponse);
    }

    /**
     * 获取用户详情（含简历信息）
     *
     * @param id 用户 ID
     * @return 用户详情 DTO
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        UserDetailResponse r = new UserDetailResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setNickname(user.getNickname());
        r.setPhone(user.getPhone());
        r.setRole(user.getRole());
        r.setStatus(user.getStatus());
        r.setCreatedAt(user.getCreatedAt());

        Resume resume = resumeRepository.findByUserId(id).orElse(null);
        long resumeCount = resumeRepository.countByUserId(id);
        long appCount = applicationRepository.countByUserId(id);
        r.setResumeCount(resumeCount);
        r.setApplicationCount(appCount);

        if (resume != null) {
            r.setEmail(resume.getEmail());
            UserDetailResponse.ResumeInfo info = new UserDetailResponse.ResumeInfo();
            info.setId(resume.getId());
            info.setRealName(resume.getRealName());
            info.setEducation(resume.getEducation());
            info.setSchool(resume.getSchool());
            info.setSkills(resume.getSkills());
            r.setResumeList(List.of(info));
        }
        return r;
    }

    /**
     * 封禁用户
     *
     * 将用户状态设置为 banned，被禁用户无法登录。
     * 操作记录到 audit_log。
     *
     * @param adminId 操作管理员 ID
     * @param userId  被禁用户 ID
     */
    @Transactional
    public void banUser(Long adminId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        user.setStatus("banned");
        userRepository.save(user);
        auditLogService.log(adminId, "ban_user", "user", userId, null);
    }

    /**
     * 解封用户
     *
     * 将用户状态恢复为 active。
     * 操作记录到 audit_log。
     *
     * @param adminId 操作管理员 ID
     * @param userId  被解封用户 ID
     */
    @Transactional
    public void unbanUser(Long adminId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        user.setStatus("active");
        userRepository.save(user);
        auditLogService.log(adminId, "unban_user", "user", userId, null);
    }

    public void resetPassword(Long adminId, Long userId, String newPassword) {
        userService.resetPassword(userId, newPassword);
        auditLogService.log(adminId, "reset_password", "user", userId, "管理员重置密码");
    }

    /**
     * 将 User 实体转换为 UserListResponse DTO
     */
    private UserListResponse toListResponse(User u) {
        UserListResponse r = new UserListResponse();
        r.setId(u.getId());
        r.setUsername(u.getUsername());
        r.setNickname(u.getNickname());
        r.setPhone(u.getPhone());
        r.setRole(u.getRole());
        r.setStatus(u.getStatus());
        r.setCreatedAt(u.getCreatedAt());
        try {
            r.setResumeCount(resumeRepository.countByUserId(u.getId()));
            r.setApplicationCount(applicationRepository.countByUserId(u.getId()));
        } catch (Exception e) {
            r.setResumeCount(0);
            r.setApplicationCount(0);
        }
        return r;
    }
}
