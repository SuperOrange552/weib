package com.weib.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CaptchaServiceTest {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
    HttpSession session = mock(HttpSession.class);
    CaptchaService service;

    @BeforeEach void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        when(session.getId()).thenReturn("sid");
        service = new CaptchaService(redis);
    }

    @Test void issueStoresCodeForTwoMinutesAndAddsCooldown() {
        when(values.setIfAbsent(eq("captcha:cooldown:sid"), eq("1"), eq(Duration.ofSeconds(5)))).thenReturn(true);
        when(values.increment("captcha:rate:GET:/captcha:1.2.3.4")).thenReturn(1L);
        var result = service.issue(session, "1.2.3.4", "ABCD");
        assertTrue(result.success());
        verify(values).set("captcha:code:sid", "ABCD", 120, TimeUnit.SECONDS);
        verify(redis).delete("captcha:fail:sid");
    }

    @Test void issueRejectsRefreshDuringCooldown() {
        when(values.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(5)))).thenReturn(false);
        when(redis.getExpire("captcha:cooldown:sid", TimeUnit.SECONDS)).thenReturn(4L);
        var result = service.issue(session, "1.2.3.4", "ABCD");
        assertFalse(result.success()); assertEquals(4, result.retryAfterSeconds());
    }

    @Test void correctCodeIsOneTime() {
        when(values.get("captcha:code:sid")).thenReturn("ABCD");
        assertEquals(CaptchaService.VerifyStatus.VALID, service.verify(session, "abcd"));
        verify(redis).delete(java.util.List.of("captcha:code:sid", "captcha:fail:sid"));
    }

    @Test void fifthWrongAttemptInvalidatesCode() {
        when(values.get("captcha:code:sid")).thenReturn("ABCD");
        when(values.increment("captcha:fail:sid")).thenReturn(5L);
        assertEquals(CaptchaService.VerifyStatus.LOCKED, service.verify(session, "xxxx"));
        verify(redis).delete(java.util.List.of("captcha:code:sid", "captcha:fail:sid"));
    }
}
