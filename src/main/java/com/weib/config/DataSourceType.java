package com.weib.config;

/**
 * 数据源路由类型
 *
 * MASTER：写操作（INSERT/UPDATE/DELETE）
 * REPLICA：读操作（SELECT）
 */
public enum DataSourceType {
    MASTER,
    REPLICA
}
