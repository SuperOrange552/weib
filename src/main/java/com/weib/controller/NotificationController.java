package com.weib.controller;

import com.weib.dto.Result;
import com.weib.entity.Notification;
import com.weib.entity.User;
import com.weib.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public String notificationsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        List<Notification> notifications = notificationService.getUserNotifications(user.getId());
        model.addAttribute("notifications", notifications);
        return "notifications";
    }

    @PostMapping("/api/notifications/read-all")
    @com.weib.security.Idempotent
    @ResponseBody
    public Result<?> markAllRead(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error("请先登录");
        notificationService.markAllAsRead(user.getId());
        return Result.success();
    }

    @PostMapping("/api/notifications/{id}/read")
    @com.weib.security.Idempotent
    @ResponseBody
    public Result<?> markRead(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error("请先登录");
        try {
            notificationService.markAsRead(id, user.getId());
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(403, e.getMessage());
        }
    }
}
