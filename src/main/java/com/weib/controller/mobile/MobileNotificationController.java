package com.weib.controller.mobile;
import com.weib.dto.Result; import com.weib.entity.MobilePushToken; import com.weib.identity.ActiveIdentity; import com.weib.identity.ActiveIdentityResolver;
import com.weib.notification.NotificationEventService; import com.weib.repository.MobilePushTokenRepository;
import jakarta.servlet.http.HttpSession; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/mobile/notifications") @RequiredArgsConstructor
public class MobileNotificationController {
 private final NotificationEventService events; private final MobilePushTokenRepository tokens; private final ActiveIdentityResolver identities;
 @GetMapping public Result<?> list(@RequestParam(defaultValue="0") long afterEventId,@RequestParam(defaultValue="100") int limit,HttpSession session){ActiveIdentity i=identities.current(session);return Result.success(events.after(i.userId(),i.role(),afterEventId,limit));}
 @PostMapping("/{id}/read") public Result<Void> read(@PathVariable Long id,HttpSession session){ActiveIdentity i=identities.current(session);events.markRead(i.userId(),i.role(),id);return Result.success();}
 @PostMapping("/push-token") public Result<Void> token(@RequestBody Map<String,String> body,HttpSession session){ActiveIdentity i=identities.current(session);String installation=required(body.get("installationId"));String token=required(body.get("pushToken"));MobilePushToken p=tokens.findByInstallationId(installation).orElseGet(MobilePushToken::new);p.setInstallationId(installation);p.setPushToken(token);p.setUserId(i.userId());p.setActiveRole(i.role());p.setStatus("ACTIVE");tokens.save(p);return Result.success();}
 private String required(String value){if(value==null||value.isBlank())throw new IllegalArgumentException("参数不能为空");return value.trim();}
}
