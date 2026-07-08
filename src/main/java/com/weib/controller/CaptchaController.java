package com.weib.controller;

import com.weib.service.CaptchaService;
import com.weib.util.CaptchaUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CaptchaController {
    private final CaptchaService captchaService;

    @GetMapping("/captcha") @ResponseBody
    public ResponseEntity<?> captcha(HttpSession session, HttpServletRequest request) throws IOException {
        String code=CaptchaUtil.generateCode();
        var issued=captchaService.issue(session,clientIp(request),code);
        if (!issued.success()) return ResponseEntity.status(429).contentType(MediaType.APPLICATION_JSON)
                .header("Retry-After",String.valueOf(issued.retryAfterSeconds()))
                .body(Map.of("message",issued.message(),"retryAfterSeconds",issued.retryAfterSeconds()));
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        ImageIO.write(CaptchaUtil.generateImage(code),"png",out);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .header("X-Captcha-Expires-In",String.valueOf(CaptchaService.CODE_TTL_SECONDS)).body(out.toByteArray());
    }
    private String clientIp(HttpServletRequest request) {
        String ip=request.getHeader("X-Forwarded-For");
        if (ip!=null && !ip.isBlank()) return ip.split(",")[0].trim();
        ip=request.getHeader("X-Real-IP");
        return ip==null||ip.isBlank()?request.getRemoteAddr():ip.trim();
    }
}