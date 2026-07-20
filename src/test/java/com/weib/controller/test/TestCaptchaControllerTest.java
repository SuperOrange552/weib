package com.weib.controller.test;

import com.weib.service.CaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TestCaptchaControllerTest {

    private CaptchaService captchaService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        captchaService = mock(CaptchaService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new TestCaptchaController(captchaService, "0123456789abcdef-test-key")
        ).build();
    }

    @Test
    void returnsCaptchaTextWhenAccessKeyIsValid() throws Exception {
        when(captchaService.issue(any(), anyString(), anyString()))
                .thenReturn(CaptchaService.IssueResult.ok());

        mvc.perform(get("/api/test/captcha")
                        .header(TestCaptchaController.ACCESS_KEY_HEADER, "0123456789abcdef-test-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.captcha").isString())
                .andExpect(jsonPath("$.data.captcha").value(org.hamcrest.Matchers.matchesPattern("[A-HJ-NP-Z2-9]{4}")))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(120));
    }

    @Test
    void hidesEndpointWhenAccessKeyIsMissingOrWrong() throws Exception {
        mvc.perform(get("/api/test/captcha"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mvc.perform(get("/api/test/captcha")
                        .header(TestCaptchaController.ACCESS_KEY_HEADER, "wrong-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        verifyNoInteractions(captchaService);
    }

    @Test
    void keepsCaptchaRateLimitBehavior() throws Exception {
        when(captchaService.issue(any(), anyString(), anyString()))
                .thenReturn(CaptchaService.IssueResult.limited(5, "验证码刷新过于频繁"));

        mvc.perform(get("/api/test/captcha")
                        .header(TestCaptchaController.ACCESS_KEY_HEADER, "0123456789abcdef-test-key"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(5));
    }
}
