package com.weib.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 数据源配置 —— 读写分离
 *
 * 开发环境：REPLICA 不启用 → 自动使用 MASTER 兜底
 * 生产环境：REPLICA 启用 → 读请求路由到从库
 *
 * 切换：application.yml → weib.datasource.replica.enabled: true/false
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    /**
     * 主数据源（写操作）
     * 使用 Spring Boot 的 DataSourceProperties 读取 spring.datasource.*
     */
    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource masterDataSource(DataSourceProperties properties) {
        log.info("主数据源 (MASTER) 已初始化: {}", properties.getUrl());
        return properties.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }

    /**
     * 从数据源 Properties（读操作）
     */
    @Bean(name = "replicaProperties")
    @ConfigurationProperties(prefix = "weib.datasource.replica")
    @ConditionalOnProperty(name = "weib.datasource.replica.enabled", havingValue = "true")
    public DataSourceProperties replicaProperties() {
        return new DataSourceProperties();
    }

    /**
     * 从数据源（读操作）— 独立连接
     */
    @Bean(name = "replicaDataSource")
    @ConditionalOnProperty(name = "weib.datasource.replica.enabled", havingValue = "true")
    public DataSource replicaDataSource(@Qualifier("replicaProperties") DataSourceProperties props) {
        log.info("从数据源 (REPLICA) 已初始化: {}", props.getUrl());
        return props.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }

    /**
     * 从数据源兜底（开发模式）
     * 当独立从库未启用时，直接复用主数据源
     */
    @Bean(name = "replicaDataSource")
    @ConditionalOnProperty(name = "weib.datasource.replica.enabled", havingValue = "false", matchIfMissing = true)
    public DataSource replicaFallback(@Qualifier("masterDataSource") DataSource master) {
        log.info("从数据源 (REPLICA) 回退到主库 (开发模式)");
        return master;
    }

    /**
     * 路由数据源（对外唯一入口）
     */
    @Primary
    @Bean(name = "dataSource")
    public DataSource routingDataSource(
            @Qualifier("masterDataSource") DataSource master,
            @Qualifier("replicaDataSource") DataSource replica) {
        log.info("路由数据源已就绪: MASTER + REPLICA");
        return new RoutingDataSource(master, replica);
    }
}
