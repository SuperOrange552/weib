package com.weib.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 读写分离路由切面
 *
 * 拦截所有 @Transactional 注解的方法：
 *   readOnly=true  → 路由到 REPLICA（从库）
 *   readOnly=false → 路由到 MASTER（主库，默认）
 *
 * @Order(0) 确保在 @Transactional 切面之前执行
 */
@Aspect
@Component
@Order(0)
public class ReadOnlyRouteAspect {

    private static final Logger log = LoggerFactory.getLogger(ReadOnlyRouteAspect.class);

    /**
     * 拦截 @Transactional 注解的方法，根据 readOnly 属性决定数据源路由
     */
    @Around("@annotation(transactional)")
    public Object routeDataSource(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable {
        DataSourceType previousType = DataSourceContextHolder.get();
        DataSourceType targetType = transactional.readOnly()
            ? DataSourceType.REPLICA
            : DataSourceType.MASTER;

        if (previousType != targetType) {
            DataSourceContextHolder.set(targetType);
            log.debug("数据源切换: {} → {} (方法: {}.{})",
                previousType, targetType,
                pjp.getSignature().getDeclaringType().getSimpleName(),
                pjp.getSignature().getName());
        }

        try {
            return pjp.proceed();
        } finally {
            // 恢复之前的数据源类型（支持嵌套事务场景）
            if (previousType != targetType) {
                DataSourceContextHolder.set(previousType);
            }
        }
    }
}
