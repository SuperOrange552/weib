package com.weib.controller;

import com.weib.util.CaptchaUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class CaptchaController {

    private static final String SESSION_KEY = "captcha_code";

    @GetMapping("/captcha")
    public void captcha(HttpSession session, HttpServletResponse response) throws IOException {
        String code = CaptchaUtil.generateCode();
        session.setAttribute(SESSION_KEY, code);

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        ImageIO.write(CaptchaUtil.generateImage(code), "png", response.getOutputStream());
    }

    public static boolean verify(HttpSession session, String input) {
        if (input == null) return false;
        String code = (String) session.getAttribute(SESSION_KEY);
        if (code != null && code.equalsIgnoreCase(input.trim())) {
            session.removeAttribute(SESSION_KEY); // 验证成功才清除，防止重放
            return true;
        }
        return false;
    }
}
