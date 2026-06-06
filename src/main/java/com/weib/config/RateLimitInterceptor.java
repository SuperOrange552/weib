package com.weib.config;

import com.weib.annotation.RateLimit;
import com.weib.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 频率限制拦截器
 *
 * 基于 @RateLimit 注解和 Redis 计数器实现。
 * Redis 不可用时自动降级放行，避免阻塞正常业务。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String REDIS_PREFIX = "rate_limit:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        // Redis 不可用：降级放行
        if (redisTemplate == null) {
            return true;
        }

        String key = buildKey(request, rateLimit);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                // 首次设置过期时间
                redisTemplate.expire(key, rateLimit.windowSeconds(), TimeUnit.SECONDS);
            }

            if (count != null && count > rateLimit.maxRequests()) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(429);
                response.getWriter().write("{\"code\":429,\"msg\":\"操作太频繁，请稍后再试\",\"data\":null}");
                return false;
            }
        } catch (Exception e) {
            // Redis 异常：降级放行
            log.warn("限流Redis操作异常，降级放行: {}", e.getMessage());
        }
        return true;
    }

    private String buildKey(HttpServletRequest request, RateLimit rateLimit) {
        String suffix;
        String keyType = rateLimit.key();
        if ("ip".equals(keyType)) {
            suffix = getClientIp(request);
        } else if ("user".equals(keyType)) {
            User user = (User) request.getSession(false) != null
                    ? (User) request.getSession(false).getAttribute("user") : null;
            suffix = user != null ? String.valueOf(user.getId()) : getClientIp(request);
        } else {
            // 默认：类名+方法名
            suffix = request.getMethod() + ":" + request.getRequestURI();
        }
        return REDIS_PREFIX + suffix;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
