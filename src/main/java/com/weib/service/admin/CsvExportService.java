package com.weib.service.admin;

import com.weib.dto.admin.UserListResponse;
import com.weib.entity.AuditLog;
import com.weib.entity.User;
import com.weib.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CSV 导出服务
 *
 * 提供用户列表和操作日志的 CSV 格式导出。
 * CSV 文件带 UTF-8 BOM 头，确保在 Excel 中正确显示中文。
 */
@Service
public class CsvExportService {
    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public CsvExportService(AdminUserService adminUserService, AuditLogService auditLogService,
                            UserRepository userRepository) {
        this.adminUserService = adminUserService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    /**
     * 导出用户列表为 CSV
     *
     * CSV 列：用户名,昵称,角色,状态,投递数,注册时间
     *
     * @param role    角色筛选
     * @param status  状态筛选
     * @param keyword 关键词搜索
     * @return CSV 字符串（UTF-8 with BOM）
     */
    public String exportUsers(String role, String status, String keyword) {
        StringWriter sw = new StringWriter();
        // BOM for Excel UTF-8
        sw.write("\uFEFF用户名,昵称,角色,状态,投递数,注册时间\n");
        Page<UserListResponse> page = adminUserService.listUsers(role, status, keyword,
                PageRequest.of(0, Integer.MAX_VALUE));
        for (UserListResponse u : page.getContent()) {
            sw.write(String.format("%s,%s,%s,%s,%d,%s\n",
                    csvEscape(u.getUsername()), csvEscape(nn(u.getNickname())),
                    u.getRole(), u.getStatus(), u.getApplicationCount(), u.getCreatedAt()));
        }
        return sw.toString();
    }

    /**
     * 导出操作日志为 CSV
     *
     * CSV 列：操作人,操作类型,目标类型,目标ID,理由,时间
     *
     * @param action    操作类型筛选
     * @param adminId   管理员 ID 筛选
     * @param startDate 起始时间
     * @param endDate   结束时间
     * @return CSV 字符串（UTF-8 with BOM）
     */
    public String exportAuditLogs(String action, Long adminId, LocalDateTime start, LocalDateTime end) {
        StringWriter sw = new StringWriter();
        sw.write("\uFEFF操作人,操作类型,目标类型,目标ID,理由,时间\n");
        Page<AuditLog> page = auditLogService.searchLogs(action, adminId, start, end,
                PageRequest.of(0, Integer.MAX_VALUE));
        // 批量加载管理员名称，避免 N+1 查询
        List<Long> adminIds = page.getContent().stream().map(AuditLog::getAdminId).distinct().toList();
        Map<Long, String> nameMap = userRepository.findAllById(adminIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        for (AuditLog log : page.getContent()) {
            String adminName = nameMap.getOrDefault(log.getAdminId(), "未知");
            sw.write(String.format("%s,%s,%s,%d,%s,%s\n",
                    csvEscape(adminName), log.getAction(), log.getTargetType(),
                    log.getTargetId() != null ? log.getTargetId() : 0,
                    csvEscape(nn(log.getReason())), log.getCreatedAt()));
        }
        return sw.toString();
    }

    /**
     * CSV 字段转义 + 公式注入防护
     * 1. 双引号转义；2. 以 = + - @ 开头的值前缀单引号防 Excel 公式注入
     */
    private String csvEscape(String s) {
        if (s == null || s.isEmpty()) return "\"\"";
        String escaped = s.replace("\"", "\"\"");
        char first = escaped.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            escaped = "'" + escaped;
        }
        return "\"" + escaped + "\"";
    }

    /**
     * null-safe 字符串转换
     */
    private String nn(String s) {
        return s != null ? s : "";
    }
}
