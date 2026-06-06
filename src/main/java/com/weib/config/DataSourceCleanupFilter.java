package com.weib.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 数据源上下文清理过滤器
 *
 * 每个 HTTP 请求处理完成后，清理 DataSourceContextHolder 中的
 * ThreadLocal 数据源路由标记，防止线程池复用时造成上下文泄漏。
 *
 * @Order(HIGHEST_PRECEDENCE + 1) 确保在其他 Filter 之后执行
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DataSourceCleanupFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(DataSourceCleanupFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
