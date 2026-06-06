package com.weib.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 动态路由数据源
 *
 * 继承 Spring 的 AbstractRoutingDataSource，根据 DataSourceContextHolder
 * 中保存的类型决定当前操作使用主库还是从库。
 *
 * 调用链：
 *   Service 方法 (@Transactional(readOnly=true))
 *     → ReadOnlyRouteAspect 拦截
 *       → DataSourceContextHolder.set(REPLICA)
 *         → JPA/Hibernate getConnection()
 *           → RoutingDataSource.determineCurrentLookupKey()
 *             → DataSourceContextHolder.get()
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(RoutingDataSource.class);

    public RoutingDataSource(DataSource master, DataSource replica) {
        super.setDefaultTargetDataSource(master);
        super.setTargetDataSources(Map.of(
            DataSourceType.MASTER, master,
            DataSourceType.REPLICA, replica
        ));
        super.afterPropertiesSet();
        log.info("读写分离数据源已初始化: MASTER + REPLICA");
    }

    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType type = DataSourceContextHolder.get();
        if (log.isDebugEnabled()) {
            log.debug("当前数据源路由: {}", type);
        }
        return type;
    }
}
