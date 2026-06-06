package com.weib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口频率限制注解
 * 
 * 使用 Redis 计数器实现滑动窗口限流。
 * Redis 不可用时自动降级放行，不阻塞业务。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 时间窗口内最大请求数 */
    int maxRequests() default 10;

    /** 时间窗口（秒） */
    int windowSeconds() default 60;

    /** 限流 key 类型：空=类名+方法名, "ip"=客户端IP, "user"=登录用户ID */
    String key() default "";
}
