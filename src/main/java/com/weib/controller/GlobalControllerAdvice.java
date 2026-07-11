package com.weib.controller;

import com.weib.entity.User;
import com.weib.service.FavoriteJobService;
import com.weib.service.MessageService;
import com.weib.service.NotificationService;
import com.weib.identity.ActiveIdentityResolver;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final MessageService messageService;
    private final NotificationService notificationService;
    private final FavoriteJobService favoriteJobService;
    private final ActiveIdentityResolver activeIdentityResolver;

    @Value("${amap.key}")
    private String amapKey;

    @ModelAttribute("amapKey")
    public String amapKey() {
        return amapKey;
    }

    @ModelAttribute("unreadMsgCount")
    public int unreadMsgCount(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return 0;
        try {
            return messageService.getTotalUnreadCount(user.getId(), activeIdentityResolver.current(session).role());
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("unreadNotifCount")
    public int unreadNotifCount(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return 0;
        try {
            return notificationService.getUnreadCount(user.getId());
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("favoriteJobIds")
    public Set<Long> favoriteJobIds(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !activeIdentityResolver.hasRole(session, "SEEKER")) return Set.of();
        try {
            return favoriteJobService.getUserFavorites(user.getId())
                    .stream().map(f -> f.getJobId()).collect(Collectors.toSet());
        } catch (Exception e) {
            return Set.of();
        }
    }
}
