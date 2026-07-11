package com.weib.controller.mobile;

import com.weib.annotation.RateLimit;
import com.weib.dto.Result;
import com.weib.dto.mobile.MobileLoginRequest;
import com.weib.dto.mobile.MobileLoginResponse;
import com.weib.entity.User;
import com.weib.exception.RoleNotEnabledException;
import com.weib.security.CredentialPolicy;
import com.weib.service.CaptchaService;
import com.weib.service.IdentityService;
import com.weib.service.UserService;
import com.weib.session.ClientType;
import com.weib.session.SessionRegistryService;
import com.weib.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/mobile/auth")
public class MobileAuthController {
    private static final long TOKEN_EXPIRES_IN_SECONDS = 7200;

    private final UserService userService;
    private final CaptchaService captchaService;
    private final JwtUtil jwtUtil;
    private final IdentityService identityService;
    private final SessionRegistryService sessionRegistry;

    public MobileAuthController(UserService userService, CaptchaService captchaService, JwtUtil jwtUtil,
                                IdentityService identityService, SessionRegistryService sessionRegistry) {
        this.userService = userService;
        this.captchaService = captchaService;
        this.jwtUtil = jwtUtil;
        this.identityService = identityService;
        this.sessionRegistry = sessionRegistry;
    }

    @RateLimit(maxRequests = 10, windowSeconds = 60, key = "ip")
    @PostMapping("/login")
    public Result<MobileLoginResponse> login(@Valid @RequestBody MobileLoginRequest body,
                                              HttpSession session, HttpServletRequest request) {
        CaptchaService.VerifyStatus captchaStatus = captchaService.verify(session, body.captcha());
        if (captchaStatus != CaptchaService.VerifyStatus.VALID) {
            return Result.error(captchaMessage(captchaStatus));
        }
        if (!CredentialPolicy.validLoginInput(body.username(), body.password())) {
            return Result.error("账号或密码格式不正确");
        }

        User user = userService.login(body.username().trim(), body.password()).orElse(null);
        if (user == null) {
            if (userService.isAccountLocked(body.username().trim())) {
                return Result.error(423, "账号已临时锁定，请稍后重试");
            }
            return Result.error(401, "账号或密码错误");
        }
        if ("admin".equalsIgnoreCase(user.getRole())) {
            return Result.error(403, "App仅支持求职者和招聘者登录");
        }
        if ("banned".equalsIgnoreCase(user.getStatus())) {
            return Result.error(403, "账号已被封禁，可在申诉页面提交材料");
        }

        String requestedRole = body.selectedRole();
        if (requestedRole == null || requestedRole.isBlank()) requestedRole = user.getRole();
        final String activeRole;
        try {
            activeRole = identityService.requireEnabledRole(user.getId(), requestedRole);
        } catch (RoleNotEnabledException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }

        session.invalidate();
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("user", user);
        newSession.setAttribute("username", user.getUsername());
        newSession.setAttribute("activeRole", activeRole);
        newSession.setAttribute("clientType", "MOBILE");
        String sid = UUID.randomUUID().toString();
        newSession.setAttribute("sid", sid);
        sessionRegistry.register(user.getId(), ClientType.MOBILE, sid, activeRole,
                Integer.toHexString(String.valueOf(request.getHeader("User-Agent")).hashCode()));
        String csrfToken = com.weib.config.CsrfInterceptor.generateCsrfToken(newSession);
        newSession.setAttribute("csrf_token", csrfToken);

        String jwt = jwtUtil.generateToken(user.getId(), user.getUsername(), activeRole, sid, "MOBILE");
        return Result.success(new MobileLoginResponse(jwt, "Bearer", TOKEN_EXPIRES_IN_SECONDS,
                toMobileUser(user, activeRole)));
    }

    @GetMapping("/me")
    public Result<MobileLoginResponse.MobileUser> me(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "未登录或登录已过期");
        String activeRole = (String) session.getAttribute("activeRole");
        if (activeRole == null || activeRole.isBlank()) activeRole = user.getRole();
        if (!"SEEKER".equalsIgnoreCase(activeRole) && !"BOSS".equalsIgnoreCase(activeRole)) {
            return Result.error(403, "App仅支持求职者和招聘者登录");
        }
        return Result.success(toMobileUser(user, activeRole));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.success();
    }

    private MobileLoginResponse.MobileUser toMobileUser(User user, String activeRole) {
        String canonical = activeRole.toUpperCase(Locale.ROOT);
        return new MobileLoginResponse.MobileUser(user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatar(), canonical.toLowerCase(Locale.ROOT), canonical);
    }

    private String captchaMessage(CaptchaService.VerifyStatus status) {
        return switch (status) {
            case EXPIRED -> "验证码已过期，请刷新后重试";
            case LOCKED -> "验证码错误次数过多，请刷新后重试";
            default -> "验证码错误";
        };
    }
}
