package com.weib.service.admin;

import com.weib.entity.AuditLog;
import com.weib.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核操作日志服务
 *
 * 提供操作日志的写入、高级搜索和最近日志查询功能。
 * 用于操作追溯和安全审计。
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 记录一条操作日志
     *
     * @param adminId    执行操作的管理员用户 ID
     * @param action     操作类型（approve_company / reject_company / ban_user 等）
     * @param targetType 操作目标类型（company / job / user / admin）
     * @param targetId   操作目标 ID（可为 null）
     * @param reason     操作原因/备注
     */
    @Transactional
    public void log(Long adminId, String action, String targetType, Long targetId, String reason) {
        AuditLog log = new AuditLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setReason(reason);
        auditLogRepository.save(log);
    }

    /**
     * 高级搜索操作日志
     *
     * 支持按操作类型、管理员 ID、时间范围组合查询，所有参数均可选。
     *
     * @param action    操作类型（可选）
     * @param adminId   管理员 ID（可选）
     * @param startDate 起始时间（可选）
     * @param endDate   结束时间（可选）
     * @param pageable  分页参数
     * @return 分页日志结果
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> searchLogs(String action, Long adminId,
                                      LocalDateTime startDate, LocalDateTime endDate,
                                      Pageable pageable) {
        return auditLogRepository.searchLogs(action, adminId, startDate, endDate, pageable);
    }

    /**
     * 获取最近操作日志（用于仪表盘快速预览）
     *
     * @param limit 返回条数上限
     * @return 最近 N 条操作日志
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.searchLogs(null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }
}
