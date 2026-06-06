package com.weib.service;

import com.weib.entity.Notification;
import com.weib.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createNotification(Long userId, String type, String content, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createNotification(Long userId, String type, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setContent(content);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (!n.getUserId().equals(userId)) {
                throw new RuntimeException("无权操作此通知");
            }
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    /**
     * 创建系统通知（管理后台审核后触发）
     *
     * 当管理后台完成审核操作后，通过此方法向目标用户发送系统通知。
     * 例如：公司审核通过/驳回、职位审核通过/驳回、用户封禁等。
     *
     * @param userId    接收通知的用户 ID
     * @param type      通知类型（如 approve_company / reject_company / ban_user）
     * @param content   通知内容
     * @param relatedId 关联目标 ID（公司 ID / 职位 ID 等）
     */
    @Transactional
    public void createSystemNotification(Long userId, String type, String content, Long relatedId) {
        createNotification(userId, type, content, relatedId);
    }
}
