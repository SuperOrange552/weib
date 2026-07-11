package com.weib.controller.mobile;

import com.weib.dto.Result;
import com.weib.entity.Notification;
import com.weib.entity.User;
import com.weib.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile/common")
@RequiredArgsConstructor
public class MobileCommonController {
    private final NotificationService notificationService;
    private final MobileAccessPolicy accessPolicy;

    @GetMapping("/notifications")
    public Result<?> notifications(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || (!accessPolicy.hasRole(session, "SEEKER") && !accessPolicy.hasRole(session, "BOSS"))) {
            return Result.error(401, "未登录或登录已过期");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("unreadCount", notificationService.getUnreadCount(user.getId()));
        data.put("notifications", notificationService.getUserNotifications(user.getId()).stream()
                .map(this::notificationJson).toList());
        return Result.success(data);
    }

    private Map<String, Object> notificationJson(Notification n) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", n.getId()); map.put("type", n.getType()); map.put("content", n.getContent());
        map.put("relatedId", n.getRelatedId()); map.put("isRead", n.getIsRead()); map.put("createdAt", n.getCreatedAt());
        return map;
    }
}
