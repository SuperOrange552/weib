package com.weib.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CaptchaService {
    public static final int CODE_TTL_SECONDS=120, COOLDOWN_SECONDS=5, RATE_WINDOW_SECONDS=60, RATE_MAX_REQUESTS=10, MAX_FAILURES=5;
    private static final String FALLBACK_CODE="captcha_fallback_code", FALLBACK_EXPIRES="captcha_fallback_expires", FALLBACK_FAILS="captcha_fallback_fails";
    private final StringRedisTemplate redis;

    public record IssueResult(boolean success, int retryAfterSeconds, String message) {
        public static IssueResult ok(){ return new IssueResult(true,0,null); }
        public static IssueResult limited(int seconds,String message){ return new IssueResult(false,Math.max(1,seconds),message); }
    }
    public enum VerifyStatus { VALID, INVALID, EXPIRED, LOCKED }

    public IssueResult issue(HttpSession session,String ip,String code) {
        String sid=session.getId(), cooldown="captcha:cooldown:"+sid, rate="captcha:rate:GET:/captcha:"+ip;
        try {
            Boolean acquired=redis.opsForValue().setIfAbsent(cooldown,"1",Duration.ofSeconds(COOLDOWN_SECONDS));
            if (!Boolean.TRUE.equals(acquired)) {
                Long ttl=redis.getExpire(cooldown,TimeUnit.SECONDS);
                return IssueResult.limited(ttl==null?COOLDOWN_SECONDS:ttl.intValue(),"验证码刷新过于频繁");
            }
            Long count=redis.opsForValue().increment(rate);
            if (count!=null && count==1) redis.expire(rate,RATE_WINDOW_SECONDS,TimeUnit.SECONDS);
            if (count!=null && count>RATE_MAX_REQUESTS) {
                Long ttl=redis.getExpire(rate,TimeUnit.SECONDS);
                return IssueResult.limited(ttl==null?RATE_WINDOW_SECONDS:ttl.intValue(),"请求过于频繁，请稍后再试");
            }
            redis.opsForValue().set("captcha:code:"+sid,code,CODE_TTL_SECONDS,TimeUnit.SECONDS);
            redis.delete("captcha:fail:"+sid);
            return IssueResult.ok();
        } catch (RuntimeException ex) {
            session.setAttribute(FALLBACK_CODE,code);
            session.setAttribute(FALLBACK_EXPIRES,System.currentTimeMillis()+CODE_TTL_SECONDS*1000L);
            session.setAttribute(FALLBACK_FAILS,0);
            return IssueResult.ok();
        }
    }

    public VerifyStatus verify(HttpSession session,String input) {
        if (input==null || input.isBlank()) return VerifyStatus.INVALID;
        String sid=session.getId(), codeKey="captcha:code:"+sid, failKey="captcha:fail:"+sid;
        try {
            String code=redis.opsForValue().get(codeKey);
            if (code==null) return VerifyStatus.EXPIRED;
            if (code.equalsIgnoreCase(input.trim())) { redis.delete(List.of(codeKey,failKey)); return VerifyStatus.VALID; }
            Long failures=redis.opsForValue().increment(failKey);
            if (failures!=null && failures==1) {
                Long ttl=redis.getExpire(codeKey,TimeUnit.SECONDS);
                redis.expire(failKey,Math.max(1,ttl==null?CODE_TTL_SECONDS:ttl),TimeUnit.SECONDS);
            }
            if (failures!=null && failures>=MAX_FAILURES) { redis.delete(List.of(codeKey,failKey)); return VerifyStatus.LOCKED; }
            return VerifyStatus.INVALID;
        } catch (RuntimeException ex) { return verifyFallback(session,input); }
    }

    private VerifyStatus verifyFallback(HttpSession session,String input) {
        Object expires=session.getAttribute(FALLBACK_EXPIRES);
        String code=(String)session.getAttribute(FALLBACK_CODE);
        if (code==null || !(expires instanceof Long) || System.currentTimeMillis()>(Long)expires) { clearFallback(session); return VerifyStatus.EXPIRED; }
        if (code.equalsIgnoreCase(input.trim())) { clearFallback(session); return VerifyStatus.VALID; }
        Integer failures=(Integer)session.getAttribute(FALLBACK_FAILS); failures=failures==null?1:failures+1;
        if (failures>=MAX_FAILURES) { clearFallback(session); return VerifyStatus.LOCKED; }
        session.setAttribute(FALLBACK_FAILS,failures); return VerifyStatus.INVALID;
    }
    private void clearFallback(HttpSession s){ s.removeAttribute(FALLBACK_CODE);s.removeAttribute(FALLBACK_EXPIRES);s.removeAttribute(FALLBACK_FAILS); }
}