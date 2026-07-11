package com.weib.session;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SecuritySessionNotifier {
    private final SimpMessagingTemplate messagingTemplate;

    public void forceLogout(Long userId, LoginSlot replaced, SessionInvalidationReason reason) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/security",
                new SecurityEvent("FORCE_LOGOUT", reason.name(), replaced == null ? null : replaced.sid(),
                        replaced == null ? null : replaced.clientType(), Instant.now().toEpochMilli(), message(reason)));
    }

    private String message(SessionInvalidationReason reason) {
        return switch (reason) {
            case KICKED -> "你的账号已在其他设备登录，请检查密码是否泄露";
            case PASSWORD_CHANGED -> "密码已修改，请重新登录";
            case ACCOUNT_BANNED -> "账号已被封禁";
            case ADMIN_FORCED -> "管理员已强制该账号下线";
            case EXPIRED -> "登录已过期，请重新登录";
        };
    }

    public record SecurityEvent(String type, String reason, String sid, String clientType,
                                long occurredAt, String message) { }
}
