package com.weib.controller.mobile;

import com.weib.entity.User;
import com.weib.service.CaptchaService;
import com.weib.service.UserService;
import com.weib.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MobileAuthControllerTest {
    private UserService userService;
    private CaptchaService captchaService;
    private JwtUtil jwtUtil;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        captchaService = mock(CaptchaService.class);
        jwtUtil = mock(JwtUtil.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new MobileAuthController(userService, captchaService, jwtUtil)
        ).build();
    }

    @Test
    void rejectsInvalidCaptcha() throws Exception {
        when(captchaService.verify(any(), eq("BAD1"))).thenReturn(CaptchaService.VerifyStatus.INVALID);
        mvc.perform(post("/api/mobile/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"seeker_ahua\",\"password\":\"Secret123\",\"captcha\":\"BAD1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("验证码错误"));
    }

    @Test
    void returnsBearerTokenForSeeker() throws Exception {
        User seeker = user(7L, "seeker_ahua", "求职者阿华", "seeker");
        when(captchaService.verify(any(), eq("AB12"))).thenReturn(CaptchaService.VerifyStatus.VALID);
        when(userService.login("seeker_ahua", "Secret123")).thenReturn(Optional.of(seeker));
        when(jwtUtil.generateToken(7L, "seeker_ahua", "seeker")).thenReturn("seeker.jwt");

        mvc.perform(post("/api/mobile/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"seeker_ahua\",\"password\":\"Secret123\",\"captcha\":\"AB12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("seeker.jwt"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.user.role").value("seeker"));
    }

    @Test
    void returnsBearerTokenForBoss() throws Exception {
        User boss = user(8L, "boss_zhang", "张经理", "boss");
        when(captchaService.verify(any(), eq("CD34"))).thenReturn(CaptchaService.VerifyStatus.VALID);
        when(userService.login("boss_zhang", "Secret123")).thenReturn(Optional.of(boss));
        when(jwtUtil.generateToken(8L, "boss_zhang", "boss")).thenReturn("boss.jwt");

        mvc.perform(post("/api/mobile/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"boss_zhang\",\"password\":\"Secret123\",\"captcha\":\"CD34\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("boss.jwt"))
                .andExpect(jsonPath("$.data.user.role").value("boss"));
    }

    @Test
    void rejectsAdminBecauseAppHasNoAdminClient() throws Exception {
        User admin = user(9L, "admin", "管理员", "admin");
        when(captchaService.verify(any(), eq("EF56"))).thenReturn(CaptchaService.VerifyStatus.VALID);
        when(userService.login("admin", "Secret123")).thenReturn(Optional.of(admin));

        mvc.perform(post("/api/mobile/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Secret123\",\"captcha\":\"EF56\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("App仅支持求职者和招聘者登录"));
    }

    private User user(Long id, String username, String nickname, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus("active");
        return user;
    }
}
