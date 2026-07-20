package com.weib.controller.test;

import com.weib.annotation.RateLimit;
import com.weib.dto.Result;
import com.weib.service.CaptchaService;
import com.weib.util.CaptchaUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * API 自动化专用验证码接口。
 *
 * <p>默认不注册该 Controller。只有显式设置
 * TEST_CAPTCHA_API_ENABLED=true 时才会开放，并且请求必须携带服务端环境变量
 * TEST_CAPTCHA_ACCESS_KEY 对应的访问密钥。</p>
 */
@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "test-tools.captcha.enabled", havingValue = "true")
public class TestCaptchaController {

    public static final String ACCESS_KEY_HEADER = "X-Test-Access-Key";

    private final CaptchaService captchaService;
    private final String accessKey;

    public TestCaptchaController(CaptchaService captchaService,
                                 @Value("${test-tools.captcha.access-key:}") String accessKey) {
        this.captchaService = captchaService;
        this.accessKey = accessKey;
    }

    @RateLimit(maxRequests = 10, windowSeconds = 60, key = "ip")
    @GetMapping("/captcha")
    public ResponseEntity<?> captcha(@RequestHeader(name = ACCESS_KEY_HEADER, required = false) String providedKey,
                                     HttpSession session,
                                     HttpServletRequest request) {
        if (!validAccessKey(providedKey)) {
            // 不向未授权调用方暴露测试接口是否已启用。
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .cacheControl(CacheControl.noStore())
                    .body(Result.error(404, "接口不存在"));
        }

        String code = CaptchaUtil.generateCode();
        CaptchaService.IssueResult issued = captchaService.issue(session, clientIp(request), code);
        if (!issued.success()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(issued.retryAfterSeconds()))
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                            "code", 429,
                            "msg", issued.message(),
                            "data", Map.of("retryAfterSeconds", issued.retryAfterSeconds())
                    ));
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Result.success(new TestCaptchaPayload(code, CaptchaService.CODE_TTL_SECONDS)));
    }

    private boolean validAccessKey(String providedKey) {
        if (accessKey == null || accessKey.length() < 16 || providedKey == null) {
            return false;
        }
        return MessageDigest.isEqual(
                accessKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        return ip == null || ip.isBlank() ? request.getRemoteAddr() : ip.trim();
    }

    public record TestCaptchaPayload(String captcha, int expiresInSeconds) {
    }
}
